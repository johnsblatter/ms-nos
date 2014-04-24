package com.workshare.msnos.core.protocols.ip.udp;

import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.SingleReceipt;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.serializers.WireSerializer;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.Multicaster;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class UDPGateway implements Gateway {


    private static Logger logger = Logger.getLogger(UDPGateway.class);

    public static final String SYSP_PORT_NUM = "com.ws.nsnos.udp.port.number";
    public static final String SYSP_PORT_WIDTH = "com.ws.nsnos.udp.port.width";
    public static final String SYSP_UDP_GROUP = "com.ws.nsnos.udp.group";
    public static final String SYSP_UDP_PACKET_SIZE = "com.ws.nsnos.udp.packet.size";

    private MulticastSocket socket;
    private InetAddress group;
    private int ports[];

    private final Multicaster<Listener, Message> caster;
    private final WireSerializer sz;
    private final int packetSize;
    
    public UDPGateway(MulticastSocketFactory sockets, UDPServer server, Multicaster<Listener, Message> caster) throws IOException {
        this.caster = caster;
        this.sz = server.serializer();
        this.packetSize = Integer.getInteger(SYSP_UDP_PACKET_SIZE, 512);

        loadPorts();
        openSocket(sockets);
        startServer(server);
    }

    private void startServer(UDPServer server) {
        server.start(socket, packetSize);
        server.addListener(new Listener() {
            @Override
            public void onMessage(Message message) {
                caster.dispatch(message);
            }
        });
    }

    private void openSocket(MulticastSocketFactory sockets) throws IOException {
        for (int port : ports) {
            try {
                MulticastSocket msock = sockets.create();
                msock.setReuseAddress(true);
                msock.bind(new InetSocketAddress(port));
                socket = msock;
                logger.info("Socket opened on port: {} ", port);
                break;
            } catch (IOException ex) {
                logger.warn("Unable to open multicast socket on port: {} ", port);
            }
        }

        if (socket == null)
            throw new IOException("Unable to open socket, I tried to binding on ports " + Arrays.asList(ports));

        String groupAddressName = loadUDPGroup();
        group = InetAddress.getByName(groupAddressName);
        socket.joinGroup(group);
        logger.info("Joined group " + group);
    }

    @Override
    public void addListener(Listener listener) {
        caster.addListener(listener);
    }

    @Override
    public Set<? extends Endpoint> endpoints() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Receipt send(Message message) throws IOException {
        logger.debug(message);

        if (logger.isDebugEnabled())
            logger.debug("Broadcasting message {} ", Json.toJsonString(message));

        List<Payload> payloads;
        int fullMsgLength = sz.toBytes(message).length;
        int lengthWithoutPayload = fullMsgLength - sz.toBytes(message.getData()).length;

        if (fullMsgLength > packetSize) {
            payloads = getSplitPayloads(new ArrayList<Payload>(), message.getData(), lengthWithoutPayload);
        } else {
            payloads = Arrays.asList(message.getData());
        }

        for (Payload load : payloads) {
            Message msg = message.data(load);
            byte[] payload = sz.toBytes(msg);

            for (int port : ports) {
                DatagramPacket packet = new DatagramPacket(
                        payload,
                        payload.length,
                        group,
                        port);
                socket.send(packet);
            }
        }

        return new SingleReceipt(Status.PENDING, message);
    }

    private List<Payload> getSplitPayloads(List<Payload> payloads, Payload payload, int msgLength) throws IOException {
        Payload[] loads = payload.split();
        if (loads == null)
            throw new IOException("Unable to send message: the payload is too big and unsplittable");
        
        for (Payload load : loads) {
            if (sz.toBytes(load).length + msgLength > packetSize) {
                getSplitPayloads(payloads, load, msgLength);
            } else {
                payloads.add(load);
            }
        }
        return payloads;
    }

    private void loadPorts() {
        int port = loadBasePort();
        int width = loadPortWidth();

        ports = new int[width];
        for (int i = 0; i < width; i++) {
            ports[i] = port + i;
        }

        logger.debug("UDP mounted on ports: {} ", Arrays.toString(ports));
    }

    private Integer loadBasePort() {
        return Integer.getInteger(SYSP_PORT_NUM, 3728);
    }

    private Integer loadPortWidth() {
        return Integer.getInteger(SYSP_PORT_WIDTH, 3);
    }

    private String loadUDPGroup() {
        return System.getProperty(SYSP_UDP_GROUP, "230.31.32.33");
    }

    public WireSerializer serializer() {
        return sz;
    }
}
