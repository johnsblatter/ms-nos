package com.workshare.msnos.core.protocols.ip;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.workshare.msnos.core.protocols.ip.Network;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NetworkInterface.class, InterfaceAddress.class, Network.class})
public class NetworkTest {

    private static final byte NET_PREFIX = (byte) 24;
    private static final byte[] IPV4_ADDRESS1 = new byte[] { 10, 0, 1, 1 };
    private static final byte[] IPV4_ADDRESS2 = new byte[] { 10, 0, 0, 1 };
    private static final byte[] IPV6_ADDRESS1 = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

    private static final byte FF = (byte) 255;

    @Test
    public void shouldListAllIpv4NetworksOnAnInterface() {
        NetworkInterface nic = mock(NetworkInterface.class);
        InterfaceAddress addresses[] = new InterfaceAddress[] {
          mockInterfaceAddress(IPV4_ADDRESS2),
          mockInterfaceAddress(IPV4_ADDRESS1),
          mockInterfaceAddress(IPV6_ADDRESS1),
        };
        when(nic.getInterfaceAddresses()).thenReturn(Arrays.asList(addresses));
        
        Set<Network> nets = Network.list(nic, true);
        assertEquals(2, nets.size());
    }

    @Test
    public void shouldListAllNetworksOnAnInterface() {
        NetworkInterface nic = mock(NetworkInterface.class);
        InterfaceAddress addresses[] = new InterfaceAddress[] {
          mockInterfaceAddress(IPV4_ADDRESS2),
          mockInterfaceAddress(IPV4_ADDRESS1),
          mockInterfaceAddress(IPV6_ADDRESS1),
        };
        when(nic.getInterfaceAddresses()).thenReturn(Arrays.asList(addresses));
        
        Set<Network> nets = Network.list(nic, false);
        assertEquals(3, nets.size());
    }

    @Test
    public void shouldComputeNetmask() {
        Network net = new Network(IPV4_ADDRESS2, NET_PREFIX);
        assertArrayEquals(new byte[] { FF, FF, FF, 0 }, net.getNetmask());
    }

    @Test
    public void shouldComputeGetHostString() {
        Network net = new Network(new byte[] { 10, 0, 0, 1 }, (byte) 24);
        assertEquals("10.0.0.1", net.getHostString());
    }

    @Test
    public void shouldComputeToString() {
        Network net = new Network(new byte[] { 10, 0, 0, 1 }, (byte) 24);
        assertTrue(net.toString().contains("10.0.0.1/24"));
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
