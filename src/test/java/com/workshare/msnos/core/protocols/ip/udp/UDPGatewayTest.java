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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.Multicaster;


public class UDPGatewayTest {

    private UDPGateway gate;
	private UDPServer server;
    private MulticastSocket socket;
    private MulticastSocketFactory sockets;
    private List<Message> messages;
    
    @Before
    public void setup() throws Exception {
    	messages = new ArrayList<Message>();

    	server = mock(UDPServer.class);
    	socket = mock(MulticastSocket.class);
        sockets = mock(MulticastSocketFactory.class);
        when(sockets.create()).thenReturn(socket);
    }
    
    @Test
    public void shouldOpenTheSocket() throws Exception {
        System.setProperty(UDPGateway.SYSP_PORT_NUM,  "2727");

        gate();

        verify(socket).setReuseAddress(true);
        verify(socket).bind(new InetSocketAddress(2727));
    }

    @Test
    public void shouldJoinTheUDPGroup() throws Exception {
        System.setProperty(UDPGateway.SYSP_UDP_GROUP,  "230.31.32.33");
        
        ArgumentCaptor<InetAddress> captor = ArgumentCaptor.forClass(InetAddress.class);
        gate();
        verify(socket).joinGroup(captor.capture());

        assertEquals("230.31.32.33", captor.getValue().getHostAddress());
    }
    
    @Test
    public void shouldUseNextSocketPort() throws Exception {
        System.setProperty(UDPGateway.SYSP_PORT_NUM,  "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH,  "2");
        doThrow(new SocketException("boom!")).when(socket).bind(new InetSocketAddress(2727));

        gate();
        verify(socket).bind(new InetSocketAddress(2728));
    }
    
    @Test(expected = IOException.class)
    public void shouldBlowUpIfNoPortAvailable() throws Exception {
        System.setProperty(UDPGateway.SYSP_PORT_NUM,  "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH,  "1");
        doThrow(new SocketException("boom!")).when(socket).bind(new InetSocketAddress(2727));

		gate();
	}

    @Test
    public void shouldSendAMessageTroughTheSocket() throws Exception {
        Message message = Utils.newSampleMessage();
        gate().send(message);

        List<DatagramPacket> packets = getSentPackets();
        assertPacketValid(message, packets.get(0));
    }

    @Test
    public void shouldSendAMessageToEachPort() throws Exception {
        System.setProperty(UDPGateway.SYSP_UDP_GROUP,  "230.31.32.33");
        System.setProperty(UDPGateway.SYSP_PORT_NUM,  "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH,  "3");
        
        Message message = Utils.newSampleMessage();
        gate().send(message);

        List<DatagramPacket> packets = getSentPackets();
        assertEquals(3, packets.size());
        
        int port = 2727;
        for (DatagramPacket packet: packets) {
            assertPacketValid(message, packet);
            assertEquals(port++, packet.getPort());
            assertEquals(InetAddress.getByName("230.31.32.33"), packet.getAddress());
        }
    }

    @Test
    public void shouldStartServer() throws Exception {
        gate();
        verify(server).start(socket, UDPGateway.PACKET_SIZE);
    }

    @Test
    public void shouldInvokeListenerOnMessageReceived() throws Exception {
        gate().addListener(new Listener(){
			@Override
			public void onMessage(Message message) {
				messages.add(message);
			}});
        
        ArgumentCaptor<Listener> serverListener = ArgumentCaptor.forClass(Listener.class);
        verify(server).addListener(serverListener.capture());

        Message message = Utils.newSampleMessage();
        serverListener.getValue().onMessage(message);
        
        assertEquals(1, messages.size());
        assertEquals(message, messages.get(0));
    }

    // public void shouldInvokeListenerOnMessagesAddressedToMe() 
    // public void shouldInvokeListenerOnMessagesAddressedToMyCloud() 
    // public void shouldNOTInvokeListenerOnMessagesAddressedToSomeoneElse() 
    
    private void assertPacketValid(Message message, final DatagramPacket packet) {
        byte[] actuals = packet.getData();
        byte[] expecteds = Json.toBytes(message);
        assertArrayEquals(expecteds, actuals);
    }

    private List<DatagramPacket> getSentPackets() throws IOException {
        ArgumentCaptor<DatagramPacket> packetCaptor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(socket, atLeastOnce()).send(packetCaptor.capture());
        return packetCaptor.getAllValues();
    }

	private UDPGateway gate() throws IOException {
		if (gate == null)
			gate= new UDPGateway(sockets, server, synchronousMulticaster());

		return gate;
	}

	private Multicaster<Listener, Message> synchronousMulticaster() {
		Executor executor = new Executor() {
			@Override
			public void execute(Runnable task) {
				task.run();
			}};

		return new Multicaster<Listener, Message>(executor){
			@Override
			protected void dispatch(Listener listener, Message message) {
				listener.onMessage(message);
			}};
	}
}
