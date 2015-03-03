package com.workshare.msnos.core.protocols.ip;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NetworkInterface.class, InterfaceAddress.class, Network.class})
public class NetworkTest {

    private static final byte NET_PREFIX = (byte) 24;
    private static final byte[] IPV4_ADDRESS1  = new byte[]{110, 0, 1, 1};
    private static final byte[] IPV4_ADDRESS2  = new byte[]{120, 0, 0, 1};
    private static final byte[] IPV6_ADDRESS1  = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static final byte[] IPV4_LOCALHOST = new byte[]{127, 0, 0, 1};

    private static final byte FF = (byte) 255;

    @Test
    public void shouldSkipIpv6AddressesIfRequested() {
        NetworkInterface nic = mock(NetworkInterface.class);
        InterfaceAddress addresses[] = new InterfaceAddress[]{
                mockInterfaceAddress(IPV4_ADDRESS2),
                mockInterfaceAddress(IPV4_ADDRESS1),
                mockInterfaceAddress(IPV6_ADDRESS1),
        };
        when(nic.getInterfaceAddresses()).thenReturn(Arrays.asList(addresses));

        Set<Network> nets = Network.list(nic, true);
        assertEquals(2, nets.size());
    }

    @Test
    public void shouldIncludeIpv6AddressesIfRequested() {
        NetworkInterface nic = mock(NetworkInterface.class);
        InterfaceAddress addresses[] = new InterfaceAddress[]{
                mockInterfaceAddress(IPV4_ADDRESS2),
                mockInterfaceAddress(IPV4_ADDRESS1),
                mockInterfaceAddress(IPV6_ADDRESS1),
        };
        when(nic.getInterfaceAddresses()).thenReturn(Arrays.asList(addresses));

        Set<Network> nets = Network.list(nic, false);
        assertEquals(3, nets.size());
    }

    @Test
    public void shouldSkipLoopbackAddresses() {
        NetworkInterface nic = mock(NetworkInterface.class);
        InterfaceAddress addresses[] = new InterfaceAddress[]{
                mockInterfaceAddress(IPV4_ADDRESS1),
                mockInterfaceAddress(IPV4_LOCALHOST),
        };
        when(nic.getInterfaceAddresses()).thenReturn(Arrays.asList(addresses));

        Set<Network> nets = Network.list(nic, true);
        assertEquals(1, nets.size());
        assertEquals(IPV4_ADDRESS1, nets.iterator().next().getAddress());
    }

    @Test
    public void shouldComputeNetmask() {
        Network net = new Network(IPV4_ADDRESS2, NET_PREFIX);
        assertArrayEquals(new byte[]{FF, FF, FF, 0}, net.getNetmask());
    }

    @Test
    public void shouldComputeGetHostString() {
        Network net = new Network(new byte[]{10, 0, 0, 1}, (byte) 24);
        assertEquals("10.0.0.1", net.getHostString());
    }

    @Test
    public void shouldComputeToString() {
        Network net = new Network(new byte[]{10, 0, 0, 1}, (byte) 24);
        assertTrue(net.toString().contains("10.0.0.1/24"));
    }

    @Test
    public void shouldComputeToStringWithNoNegativeDigitsPlease() {
        Network net = new Network(new byte[]{192 - 256, 168 - 256, 0, 1}, (byte) 24);
        assertTrue(net.toString().contains("192.168.0.1/24"));
    }

    @Test
    public void shouldComputeHashcode() {
        Network net1 = new Network(IPV4_ADDRESS2, NET_PREFIX);
        Network net2 = new Network(IPV4_ADDRESS1, NET_PREFIX);

        assertFalse(net1.hashCode() == net2.hashCode());
    }

    @Test
    public void shouldComputeEquals() {
        Network net1 = new Network(IPV4_ADDRESS2, NET_PREFIX);
        Network net2 = new Network(IPV4_ADDRESS1, NET_PREFIX);

        assertFalse(net1.equals(net2));
    }

    @Test
    public void shouldBeBuildableWithInterfaceAddress() {
        InterfaceAddress adapter = mockInterfaceAddress(IPV4_ADDRESS2, NET_PREFIX);

        Network network = new Network(adapter);
        assertEquals(network.getPrefix(), NET_PREFIX);
        assertEquals(network.getAddress(), IPV4_ADDRESS2);
    }

    @Test
    public void shouldTellIfIPIsPrivate() throws Exception {
        final byte[] privateIP = new byte[]{10, 0, 1, 1};
        InterfaceAddress adapter = mockInterfaceAddress(privateIP, NET_PREFIX);

        Network network = new Network(adapter);
        assertTrue(network.isPrivate());
    }

    @Test
    public void shouldTellIfIPIsPublic() throws Exception {
        InterfaceAddress adapter = mockInterfaceAddress(IPV4_ADDRESS1, NET_PREFIX);

        Network network = new Network(adapter);
        assertFalse(network.isPrivate());
    }

    @Test
    public void shouldAttemptToFindPublicIPifPrivate() throws Exception {
        final byte[] privateIP = new byte[]{10, 0, 1, 1};

        NetworkInterface nic = mock(NetworkInterface.class);
        InterfaceAddress addresses[] = new InterfaceAddress[]{
                mockInterfaceAddress(IPV4_ADDRESS2),
                mockInterfaceAddress(privateIP),
                mockInterfaceAddress(IPV6_ADDRESS1),
        };
        when(nic.getInterfaceAddresses()).thenReturn(Arrays.asList(addresses));
        AddressResolver resolver = mock(AddressResolver.class);

        Network.listAll(asEnumeration(nic), true, false, resolver);

        verify(resolver, atLeastOnce()).findPublicIP();
    }

    @Test
    public void shouldSkipVirtualNicsOnRequest() throws Exception {

        final NetworkInterface virtual = mock(NetworkInterface.class);
        final InterfaceAddress virtAddress = mockInterfaceAddress(IPV4_ADDRESS2);
        when(virtual.isVirtual()).thenReturn(true);
        when(virtual.getInterfaceAddresses()).thenReturn(Arrays.asList(new InterfaceAddress[]{
                virtAddress,
        }));

        final NetworkInterface eth0 = mock(NetworkInterface.class);
        final InterfaceAddress eth0Address = mockInterfaceAddress(IPV4_ADDRESS1);
        when(eth0.isVirtual()).thenReturn(false);
        when(eth0.getInterfaceAddresses()).thenReturn(Arrays.asList(new InterfaceAddress[]{
                eth0Address,
        }));

        AddressResolver resolver = mock(AddressResolver.class);
        Set<Network> res = Network.listAll(asEnumeration(eth0, virtual), true, false, resolver);

        assertEquals(1,res.size());
        assertEquals(IPV4_ADDRESS1, res.iterator().next().getAddress());
    }

    @Test
    public void shouldSkipLoopbackNics() throws Exception {

        final NetworkInterface loopback = mock(NetworkInterface.class);
        final InterfaceAddress localhost = mockInterfaceAddress(IPV4_LOCALHOST);
        when(loopback.isLoopback()).thenReturn(true);
        when(loopback.getInterfaceAddresses()).thenReturn(Arrays.asList(new InterfaceAddress[]{
                localhost,
        }));

        final NetworkInterface eth0 = mock(NetworkInterface.class);
        final InterfaceAddress eth0Address = mockInterfaceAddress(IPV4_ADDRESS1);
        when(eth0.isVirtual()).thenReturn(false);
        when(eth0.getInterfaceAddresses()).thenReturn(Arrays.asList(new InterfaceAddress[]{
                eth0Address,
        }));

        AddressResolver resolver = mock(AddressResolver.class);
        Set<Network> res = Network.listAll(asEnumeration(eth0, loopback), true, true, resolver);

        assertEquals(1,res.size());
        assertEquals(IPV4_ADDRESS1, res.iterator().next().getAddress());
    }

    @Test
    public void shouldReportOnlyTheListedNicsIfSpecified() throws Exception {
        
        System.setProperty(Network.SYSP_NET_BINDINGS, "eth0,wlan0");

        final NetworkInterface wmnet8 = mock(NetworkInterface.class);
        final InterfaceAddress virtAddress = mockInterfaceAddress(IPV4_ADDRESS2);
        when(wmnet8.getName()).thenReturn("wmnet8");
        when(wmnet8.isVirtual()).thenReturn(true);
        when(wmnet8.getInterfaceAddresses()).thenReturn(Arrays.asList(new InterfaceAddress[]{
                virtAddress,
        }));

        final NetworkInterface eth0 = mock(NetworkInterface.class);
        final InterfaceAddress eth0Address = mockInterfaceAddress(IPV4_ADDRESS1);
        when(eth0.getName()).thenReturn("eth0");
        when(eth0.isVirtual()).thenReturn(false);
        when(eth0.getInterfaceAddresses()).thenReturn(Arrays.asList(new InterfaceAddress[]{
                eth0Address,
        }));

        AddressResolver resolver = mock(AddressResolver.class);
        Set<Network> res = Network.listAll(asEnumeration(eth0, wmnet8), true, true, resolver);

        assertEquals(1,res.size());
        assertEquals(IPV4_ADDRESS1, res.iterator().next().getAddress());
    }
    
    private Enumeration<NetworkInterface> asEnumeration(NetworkInterface... nics) {
        final List<NetworkInterface> data = new ArrayList<NetworkInterface>(Arrays.asList(nics));
        return new Enumeration<NetworkInterface>() {
            @Override
            public boolean hasMoreElements() {
                return data.size() > 0;
            }

            @Override
            public NetworkInterface nextElement() {
                return data.remove(0);
            }
        };
    }

    private InterfaceAddress mockInterfaceAddress(final byte[] netAddress) {
        final InterfaceAddress intface = mockInterfaceAddress(netAddress, NET_PREFIX);
        if (netAddress.equals(IPV4_LOCALHOST)) {
            InetAddress address =  mock(InetAddress.class);
            when(address.isLoopbackAddress()).thenReturn(true);
            when(intface.getAddress()).thenReturn(address);
        }

        return intface;
    }

    private InterfaceAddress mockInterfaceAddress(final byte[] netAddress, final int netPrefix) {
        InetAddress address = mock(InetAddress.class);
        when(address.getAddress()).thenReturn(netAddress);

        InterfaceAddress adapter = mock(InterfaceAddress.class);
        when(adapter.getAddress()).thenReturn(address);
        when(adapter.getNetworkPrefixLength()).thenReturn((short) netPrefix);
        return adapter;
    }
}
