package com.workshare.msnos.core.protocols.ip;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
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
    private static final byte[] IPV4_ADDRESS1 = new byte[]{110, 0, 1, 1};
    private static final byte[] IPV4_ADDRESS2 = new byte[]{120, 0, 0, 1};
    private static final byte[] IPV6_ADDRESS1 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private static final byte FF = (byte) 255;

    @Test
    public void shouldListAllIpv4NetworksOnAnInterface() {
        NetworkInterface nic = mock(NetworkInterface.class);
        InterfaceAddress addresses[] = new InterfaceAddress[]{
                mockInterfaceAddress(IPV4_ADDRESS2),
                mockInterfaceAddress(IPV4_ADDRESS1),
                mockInterfaceAddress(IPV6_ADDRESS1),
        };
        when(nic.getInterfaceAddresses()).thenReturn(Arrays.asList(addresses));

        Set<Network> nets = Network.list(nic, true, mock(AddressResolver.class));
        assertEquals(2, nets.size());
    }

    @Test
    public void shouldListAllNetworksOnAnInterface() {
        NetworkInterface nic = mock(NetworkInterface.class);
        InterfaceAddress addresses[] = new InterfaceAddress[]{
                mockInterfaceAddress(IPV4_ADDRESS2),
                mockInterfaceAddress(IPV4_ADDRESS1),
                mockInterfaceAddress(IPV6_ADDRESS1),
        };
        when(nic.getInterfaceAddresses()).thenReturn(Arrays.asList(addresses));

        Set<Network> nets = Network.list(nic, false, mock(AddressResolver.class));
        assertEquals(3, nets.size());
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

        Network.list(nic, true, resolver);

        verify(resolver, atLeastOnce()).findPublicIP();
    }

    private InterfaceAddress mockInterfaceAddress(final byte[] netAddress) {
        return mockInterfaceAddress(netAddress, NET_PREFIX);
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
