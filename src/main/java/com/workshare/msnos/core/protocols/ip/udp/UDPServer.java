package com.workshare.msnos.core.protocols.ip.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.serializers.WireJsonSerializer;
import com.workshare.msnos.core.serializers.WireSerializer;
import com.workshare.msnos.soup.threading.Multicaster;
import com.workshare.msnos.soup.threading.ThreadFactories;

public class UDPServer {

    private static Logger logger = Logger.getLogger(UDPServer.class.getName());
    private static final String THREAD_NAME = "UDP-Server";
    
    private final ThreadFactory threads;
    private final Multicaster<Listener, Message> multicaster;
    private final WireSerializer sz;

    private Thread thread;
    private int maxPacketSize;
    private MulticastSocket socket;

    public UDPServer() {
        this(ThreadFactories.DEFAULT, new Multicaster<Listener, Message>() {
            @Override
            protected void dispatch(Listener listener, Message message) {
                listener.onMessage(message);
            }
        });
    }

    public UDPServer(ThreadFactory threads, Multicaster<Listener, Message> caster) {
        this.sz = new WireJsonSerializer();        // hard dependency to remove in future?
        this.threads = threads;
        this.multicaster = caster;
    }

    public synchronized void start(MulticastSocket socket, int maxPacketSize) {

        if (thread != null)
            throw new RuntimeException("UDPServer started two times? WTF?");

        this.socket = socket;
        this.maxPacketSize = maxPacketSize;

        thread = threads.newThread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        });

        thread.setDaemon(true);
        thread.setName(THREAD_NAME);
        thread.start();
    }

    public synchronized void stop() {

        if (!THREAD_NAME.equals(thread.getName()))
            throw new RuntimeException("UDPServer stopped two times or never started? WTF?");

        thread.setName("-ghost-");
        thread.interrupt();
    }

    private void loop() {

        byte[] buf = new byte[maxPacketSize];

        logger.info("Listening loop started on port " + socket.getLocalPort());
        while (!thread.isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                logger.log(Level.FINEST, "IOException receiving UDP packet", e);
            }

            if (thread.isInterrupted())
                break;

            try {
                process(packet);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Unable to process packet", ex);
            }
        }

        Thread.interrupted();
        logger.info("Listening loop ended!");
    }

    private void process(DatagramPacket packet) {
        Message message = (Message) sz.fromBytes(packet.getData(), 0, packet.getLength(), Message.class);
        logger.log(Level.FINEST, "Received message {} ", message.toString());

        sendToListeners(message);
    }

    private void sendToListeners(Message message) {
        multicaster.dispatch(message);
    }

    public void addListener(final Listener listener) {
        multicaster.addListener(listener);
    }

    public WireSerializer serializer() {
        return sz;
    }
}
