package com.workshare.msnos.core.protocols.ip.udp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.soup.json.Json;


public class UDPGatewayTest {

    private UDPGateway gate;
    private MulticastSocket socket;
    private MulticastSocketFactory sockets;
    
    @Before
    public void setup() throws Exception {
        socket = mock(MulticastSocket.class);
        sockets = mock(MulticastSocketFactory.class);
        when(sockets.create()).thenReturn(socket);
    }
    
    @Test
    public void shouldOpenTheSocket() throws Exception {
        System.setProperty(UDPGateway.SYSP_PORT_NUM,  "2727");

        gate = new UDPGateway(sockets, null);
        verify(socket).setReuseAddress(true);
        verify(socket).bind(new InetSocketAddress(2727));
    }
    
    @Test
    public void shouldJoinTheUDPGroup() throws Exception {
        System.setProperty(UDPGateway.SYSP_UDP_GROUP,  "230.31.32.33");
        
        ArgumentCaptor<InetAddress> captor = ArgumentCaptor.forClass(InetAddress.class);
        gate = new UDPGateway(sockets, null);
        verify(socket).joinGroup(captor.capture());

        assertEquals("230.31.32.33", captor.getValue().getHostAddress());
    }
    
    @Test
    public void shouldUseNextSocketPort() throws Exception {
        System.setProperty(UDPGateway.SYSP_PORT_NUM,  "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH,  "2");
        doThrow(new SocketException("boom!")).when(socket).bind(new InetSocketAddress(2727));

        gate = new UDPGateway(sockets, null);
        verify(socket).bind(new InetSocketAddress(2728));
    }
    
    @Test(expected = IOException.class)
    public void shouldBlowUpIfNoPortAvailable() throws Exception {
        System.setProperty(UDPGateway.SYSP_PORT_NUM,  "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH,  "1");
        doThrow(new SocketException("boom!")).when(socket).bind(new InetSocketAddress(2727));

        gate = new UDPGateway(sockets, null);
    }

    @Test
    public void shouldSendAMessageTroughTheSocket() throws Exception {
        gate = new UDPGateway(sockets, null);
        
        Message message = Utils.newSampleMessage();
        gate.send(message);

        List<DatagramPacket> packets = getSentPackets();
        assertPacketValid(message, packets.get(0));
    }

    private void assertPacketValid(Message message, final DatagramPacket packet) {
        byte[] actuals = packet.getData();
        byte[] expecteds = Json.toBytes(message);
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void shouldSendAMessageToEachPort() throws Exception {
        System.setProperty(UDPGateway.SYSP_UDP_GROUP,  "230.31.32.33");
        System.setProperty(UDPGateway.SYSP_PORT_NUM,  "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH,  "3");
        gate = new UDPGateway(sockets, null);
        
        Message message = Utils.newSampleMessage();
        gate.send(message);

        List<DatagramPacket> packets = getSentPackets();
        assertEquals(3, packets.size());
        
        int port = 2727;
        for (DatagramPacket packet: packets) {
            assertPacketValid(message, packet);
            assertEquals(port++, packet.getPort());
            assertEquals(InetAddress.getByName("230.31.32.33"), packet.getAddress());
        }
    }

    private List<DatagramPacket> getSentPackets() throws IOException {
        ArgumentCaptor<DatagramPacket> packetCaptor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(socket, atLeastOnce()).send(packetCaptor.capture());
        return packetCaptor.getAllValues();
    }
}
