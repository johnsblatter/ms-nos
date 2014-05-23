package com.workshare.msnos.core.protocols.ip.udp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
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

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.serializers.WireJsonSerializer;
import com.workshare.msnos.soup.threading.Multicaster;

public class UDPGatewayTest {

    private static final Iden ME = new Iden(Iden.Type.AGT, new UUID(123, 999));
    private static final Iden SOMEONE = new Iden(Iden.Type.AGT, UUID.randomUUID());

    private UDPGateway gate;
    private UDPServer server;
    private MulticastSocket socket;
    private MulticastSocketFactory sockets;
    private List<Message> messages;
    private Cloud cloud;

    @Before
    public void setup() throws Exception {
        messages = new ArrayList<Message>();

        server = mock(UDPServer.class);
        when(server.serializer()).thenReturn(new WireJsonSerializer());

        socket = mock(MulticastSocket.class);
        sockets = mock(MulticastSocketFactory.class);
        when(sockets.create()).thenReturn(socket);
        
        cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, UUID.randomUUID()));

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
        gate().send(cloud, message);

        List<DatagramPacket> packets = getSentPackets();
        assertPacketValid(message, packets.get(0));
    }

    @Test
    public void shouldSendAMessageToEachPort() throws Exception {
        System.setProperty(UDPGateway.SYSP_UDP_GROUP, "230.31.32.33");
        System.setProperty(UDPGateway.SYSP_PORT_NUM, "2727");
        System.setProperty(UDPGateway.SYSP_PORT_WIDTH, "3");

        Message message = UDPGatewayTest.newSampleMessage();
        gate().send(cloud,message);

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
        verify(server).start(eq(socket),anyInt());
    }

    @Test
    public void shouldInvokeListenerOnMessages() throws Exception {
        addListenerToGateway();

        Message message = UDPGatewayTest.newSampleMessage(SOMEONE,ME);
        simulateMessageFromNetwork(message);

        assertMessageReceived(message);
    }

    @Test
    public void shouldSplitUDPPacketsToMaxPacketSizeOrLess() throws IOException {
        System.setProperty(UDPGateway.SYSP_UDP_PACKET_SIZE, Integer.toString(333));
        Message message = getMessageWithPayload(new BigPayload(1000));

        gate().send(cloud,message);

        List<DatagramPacket> packets = getSentPackets();
        for (DatagramPacket datagramPacket : packets) {
            assertTrue(datagramPacket.getLength() <= 333);
        }
    }

    @Test(expected = IOException.class)
    public void shouldFailWhenUnableToSplitUDPPackets() throws IOException {
        System.setProperty(UDPGateway.SYSP_UDP_PACKET_SIZE, Integer.toString(333));
        Message message = getMessageWithPayload(new BigPayload(1000).unsplittable());

        gate().send(cloud,message);
    }

    private Message getMessageWithPayload(final BigPayload payload) {
        return new MessageBuilder(Message.Type.PRS, SOMEONE, ME).with(payload).make();
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
        gate().addListener(null, new Listener() {
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
        return newSampleMessage(src, dst);
    }

    private static Message newSampleMessage(final Iden src, final Iden dst) {
        return new MessageBuilder(Message.Type.APP, src, dst).make();
    }

    static class BigPayload implements Message.Payload {

        private byte[] data;
        private boolean splittable = true;
        
        BigPayload(int size) {
            data = new byte[size];
        }

        public BigPayload unsplittable() {
            this.splittable = false;
            return this;
        }

        @Override
        public Message.Payload[] split() {
            if (!splittable)
                return null;

            int size1 = data.length / 2;
            int size2 = data.length - size1;

            return new Message.Payload[]{new BigPayload(size1), new BigPayload(size2)};
        }

        @Override
        public boolean process(Message message, Internal internal) {
            return false;
        }
    }

}