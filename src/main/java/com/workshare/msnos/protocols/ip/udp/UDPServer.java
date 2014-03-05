package com.workshare.msnos.protocols.ip.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.json.Json;
import com.workshare.msnos.threading.Multicaster;
import com.workshare.msnos.threading.ThreadFactories;


public class UDPServer {

    private static Logger logger = Logger.getLogger(UDPServer.class.getName());

    private MulticastSocket socket;
    private int maxPacketSize;

    private Thread thread;
    private ThreadFactory threads;
    private Multicaster<Listener, Message> multicaster;

    UDPServer(MulticastSocket socket, int maxPacketSize) {
        this(ThreadFactories.DEFAULT, socket, maxPacketSize, new Multicaster<Listener, Message>(){
            @Override
            protected void dispatch(Listener listener, Message message) {
                listener.onMessage(message);
            }});
    }

    UDPServer(ThreadFactory threads, MulticastSocket socket, int maxPacketSize, Multicaster<Listener, Message> caster) {
        this.socket = socket;
        this.threads = threads;
        this.maxPacketSize = maxPacketSize;
        this.multicaster = caster;
    }

    public synchronized void start() {

        if (thread != null) 
            throw new RuntimeException("UDPServer started two times? WTF?");
            
        thread = threads.newThread(new Runnable() {
            @Override
            public void run() {
                loop();
            }});
        
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {

        if (thread == null)
            throw new RuntimeException("UDPServer stopped two times or never started? WTF?");
        
        thread.interrupt();
        thread=null;
    }

    private void loop() {
        
        byte[] buf = new byte[maxPacketSize];
        
        logger.info("Listening loop started on port "+socket.getLocalPort());
        while(!thread.isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                logger.log(Level.FINEST, "IOException receiving UDP packet", e);
            }

            if (thread.isInterrupted())
                break;
            
            ByteBuffer buffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
            Message message = (Message)Json.fromBytes(buffer.array(), Message.class);
            logger.log(Level.FINEST, "Received message "+message);

            sendToListeners(message);
        }

        Thread.interrupted();
        logger.info("Listening loop ended!");
    }

    private void sendToListeners(Message message) {
        multicaster.dispatch(message);
    }

    public void addListener(final Listener listener) {
        multicaster.addListener(listener);
    }
}
