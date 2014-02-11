package com.workshare.msnos.protocols.ip;

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

@RunWith(PowerMockRunner.class)
@PrepareForTest({NetworkInterface.class, InterfaceAddress.class, Network.class})
public class NetworkTest {

    private static final byte NET_PREFIX = (byte) 24;
    private static final byte[] NET2_ADDRESS = new byte[] { 10, 0, 1, 1 };
    private static final byte[] NET1_ADDRESS = new byte[] { 10, 0, 0, 1 };

    private static final byte FF = (byte) 255;

    @Test
    public void shouldListAllNetworksOnAnInterface() {
        NetworkInterface nic = mock(NetworkInterface.class);
        final InterfaceAddress addr1 = mockInterfaceAddress(NET1_ADDRESS);
        final InterfaceAddress addr2 = mockInterfaceAddress(NET2_ADDRESS);
        when(nic.getInterfaceAddresses()).thenReturn(Arrays.asList(addr1, addr2));
        
        Set<Network> nets = Network.list(nic);
        assertEquals(2, nets.size());
    }

    @Test
    public void shouldComputeNetmask() {
        Network net = new Network(NET1_ADDRESS, NET_PREFIX);
        assertArrayEquals(new byte[] { FF, FF, FF, 0 }, net.getNetmask());
    }

    @Test
    public void shouldComputeToString() {
        Network net = new Network(new byte[] { 10, 0, 0, 1 }, (byte) 24);
        assertTrue(net.toString().contains("10.0.0.1/24"));
    }

    @Test
    public void shouldComputeHashcode() {
        Network net1 = new Network(NET1_ADDRESS, NET_PREFIX);
        Network net2 = new Network(NET2_ADDRESS, NET_PREFIX);

        assertFalse(net1.hashCode() == net2.hashCode());
    }

    @Test
    public void shouldComputeEquals() {
        Network net1 = new Network(NET1_ADDRESS, NET_PREFIX);
        Network net2 = new Network(NET2_ADDRESS, NET_PREFIX);

        assertFalse(net1.equals(net2));
    }

    @Test
    public void shouldBeBuildableWithInterfaceAddress() {
        InterfaceAddress adapter = mockInterfaceAddress(NET1_ADDRESS, NET_PREFIX);

        Network network = new Network(adapter);
        assertEquals(network.getPrefix(), NET_PREFIX);
        assertEquals(network.getAddress(), NET1_ADDRESS);
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
