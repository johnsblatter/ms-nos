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
import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.serializers.WireJsonSerializer;
import com.workshare.msnos.soup.threading.Multicaster;

public class UDPGatewayTest {

    private static final Iden ME = new Iden(Iden.Type.AGT, new UUID(123, 999));
    private static final Iden YOU = new Iden(Iden.Type.AGT, new UUID(321, 666));
    private static final Iden SOMEONE = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final Iden MY_CLOUD = new Iden(Iden.Type.CLD, UUID.randomUUID());

    private UDPGateway gate;
    private UDPServer server;
    private MulticastSocket socket;
    private MulticastSocketFactory sockets;
    private List<Message> messages;

    private Agent rhys;

    @Before
    public void setup() throws Exception {
        messages = new ArrayList<Message>();

        server = mock(UDPServer.class);
        when(server.serializer()).thenReturn(new WireJsonSerializer());
        
        socket = mock(MulticastSocket.class);
        sockets = mock(MulticastSocketFactory.class);
        when(sockets.create()).thenReturn(socket);

        rhys = new Agent(ME.getUUID()).join(new Cloud(MY_CLOUD.getUUID()));
    }

    @Test
    public void shouldOpenTheSocket() throws Exception {
        System.setProperty(UDPGateway.SYSP_PORT_NUM, "2727");

        gate();

        verify(socket).setReuseAddress(true);
        verify(socket).bind(new InetSocketAddress(2727));
    }

    @Test
    public void shouldJoinTheUDPGroup() throws Exception {
        System.setProperty(UDPGateway.SYSP_UDP_GROUP, "230.31.32.33");

        ArgumentCaptor<InetAddress> captor = ArgumentCaptor.forClass(InetAddress.class);
        gate();
        verify(socket).joinGroup(captor.capture());

        assertEquals("230.31.32.33", captor.getValue().getHostAddress());
    }

    @Test
    public void shouldUseNextSocketPort() throws Exception {
        System.setProperty(UDPGateway.SYSP_PORT_NUM, "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH, "2");
        doThrow(new SocketException("boom!")).when(socket).bind(new InetSocketAddress(2727));

        gate();
        verify(socket).bind(new InetSocketAddress(2728));
    }

    @Test(expected = IOException.class)
    public void shouldBlowUpIfNoPortAvailable() throws Exception {
        System.setProperty(UDPGateway.SYSP_PORT_NUM, "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH, "1");
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
        System.setProperty(UDPGateway.SYSP_UDP_GROUP, "230.31.32.33");
        System.setProperty(UDPGateway.SYSP_PORT_NUM, "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH, "3");

        Message message = Utils.newSampleMessage();
        gate().send(message);

        List<DatagramPacket> packets = getSentPackets();
        assertEquals(3, packets.size());

        int port = 2727;
        for (DatagramPacket packet : packets) {
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
    public void shouldNOTInvokeListenerOnMessagesAddressedToSomeoneElse() throws Exception {
        addListenerToGateway();

        Message message = Utils.newSampleMessage().from(YOU).to(SOMEONE);
        simulateMessageFromNetwork(message);

        assertMessageNotReceived();
    }

    @Test
    public void shouldNOTInvokeListenerOnMessagesSentByMe() throws Exception {
        addListenerToGateway();

        Message message = Utils.newSampleMessage().from(ME).to(SOMEONE);
        simulateMessageFromNetwork(message);

        assertMessageNotReceived();
    }

    @Test
    public void shouldInvokeListenerOnMessagesAddressedToMe() throws Exception {
        addListenerToGateway();

        Message message = Utils.newSampleMessage().from(SOMEONE).to(ME);
        simulateMessageFromNetwork(message);

        assertMessageReceived(message);
    }

    @Test
    public void shouldInvokeListenerOnMessagesAddressedToMyCloud() throws IOException {
        addListenerToGateway();

        Message message = Utils.newSampleMessage().from(SOMEONE).to(MY_CLOUD);
        simulateMessageFromNetwork(message);

        assertMessageReceived(message);
    }

    private void simulateMessageFromNetwork(Message message) {
        ArgumentCaptor<Listener> serverListener = ArgumentCaptor.forClass(Listener.class);
        verify(server).addListener(serverListener.capture());
        serverListener.getValue().onMessage(message);
    }

    private void assertPacketValid(Message message, final DatagramPacket packet) {
        byte[] actuals = packet.getData();
        byte[] expecteds = gate.serializer().toBytes(message);
        assertArrayEquals(expecteds, actuals);
    }

    private List<DatagramPacket> getSentPackets() throws IOException {
        ArgumentCaptor<DatagramPacket> packetCaptor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(socket, atLeastOnce()).send(packetCaptor.capture());
        return packetCaptor.getAllValues();
    }

    private void assertMessageReceived(Message message) {
        assertEquals(1, messages.size());
        assertEquals(message, messages.get(0));
    }

    private void assertMessageNotReceived() {
        assertEquals(0, messages.size());
    }

    private UDPGateway gate() throws IOException {
        if (gate == null)
            gate = new UDPGateway(sockets, server, synchronousMulticaster(), rhys);

        return gate;
    }

    private void addListenerToGateway() throws IOException {
        gate().addListener(new Listener() {
            @Override
            public void onMessage(Message message) {
                messages.add(message);
            }
        });
    }

    private Multicaster<Listener, Message> synchronousMulticaster() {
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable task) {
                task.run();
            }
        };

        return new Multicaster<Listener, Message>(executor) {
            @Override
            protected void dispatch(Listener listener, Message message) {
                listener.onMessage(message);
            }
        };
    }
}
