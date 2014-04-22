package com.workshare.msnos.core.protocols.ip.udp;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.serializers.WireJsonSerializer;
import com.workshare.msnos.soup.threading.Multicaster;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UDPGatewayTest {

    private static final Iden ME = new Iden(Iden.Type.AGT, new UUID(123, 999));
    private static final Iden SOMEONE = new Iden(Iden.Type.AGT, UUID.randomUUID());

    private UDPGateway gate;
    private UDPServer server;
    private MulticastSocket socket;
    private MulticastSocketFactory sockets;
    private List<Message> messages;

    private WireJsonSerializer sz;

    @Before
    public void setup() throws Exception {
        messages = new ArrayList<Message>();

        sz = new WireJsonSerializer();

        server = mock(UDPServer.class);
        when(server.serializer()).thenReturn(new WireJsonSerializer());

        socket = mock(MulticastSocket.class);
        sockets = mock(MulticastSocketFactory.class);
        when(sockets.create()).thenReturn(socket);
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
        Message message = UDPGatewayTest.newSampleMessage();
        gate().send(message);

        List<DatagramPacket> packets = getSentPackets();
        assertPacketValid(message, packets.get(0));
    }

    @Test
    public void shouldSendAMessageToEachPort() throws Exception {
        System.setProperty(UDPGateway.SYSP_UDP_GROUP, "230.31.32.33");
        System.setProperty(UDPGateway.SYSP_PORT_NUM, "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH, "3");

        Message message = UDPGatewayTest.newSampleMessage();
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
    public void shouldInvokeListenerOnMessages() throws Exception {
        addListenerToGateway();

        Message message = UDPGatewayTest.newSampleMessage().from(SOMEONE).to(ME);
        simulateMessageFromNetwork(message);

        assertMessageReceived(message);
    }

    @Test
    public void shouldSplitUDPPacketsTo512BytesOrLess() throws IOException {
        Message message = getMessageWithBigPayload();

        gate().send(message);

        List<DatagramPacket> packets = getSentPackets();
        for (DatagramPacket datagramPacket : packets) {
            assertTrue(datagramPacket.getLength() <= 512);
        }
    }

    private Message getMessageWithBigPayload() {
        final UUID uuid = new UUID(123, 456);
        final Iden src = new Iden(Iden.Type.AGT, uuid);
        final Iden dst = new Iden(Iden.Type.CLD, uuid);
        return new Message(Message.Type.PRS, src, dst, 1, false, new BigPayload(1000)).from(SOMEONE).to(ME);
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

    private UDPGateway gate() throws IOException {
        if (gate == null)
            gate = new UDPGateway(sockets, server, synchronousMulticaster());

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

    public static Message newSampleMessage() {
        final UUID uuid = new UUID(123, 456);
        final Iden src = new Iden(Iden.Type.AGT, uuid);
        final Iden dst = new Iden(Iden.Type.CLD, uuid);
        return new Message(Message.Type.APP, src, dst, 1, false, null);
    }

    static class BigPayload implements Message.Payload {

        private byte[] data;

        BigPayload(int size) {
            data = new byte[size];
        }

        @Override
        public Message.Payload[] split() {
            int size1 = data.length / 2;
            int size2 = data.length - size1;

            return new Message.Payload[]{new BigPayload(size1), new BigPayload(size2)};
        }
    }

}