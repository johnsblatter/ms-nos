package com.workshare.msnos.protocols.ip.udp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.json.Json;

public class UDPServerTest implements Listener {

    private UDPServer server;
    private Thread thread;
    private MulticastSocket socket;
    private ArgumentCaptor<Runnable> runnableCaptor;
    private List<Message> messages;

    @Before
    public void init() {
        thread = Mockito.mock(Thread.class);
        when(thread.isInterrupted()).thenReturn(false);
        
        socket = Mockito.mock(MulticastSocket.class);

        ThreadFactory threads = Mockito.mock(ThreadFactory.class);
        server = new UDPServer(threads, socket, 512);
        server.addListener(this);
        
        runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.when(threads.newThread(runnableCaptor.capture())).thenReturn(thread);

        messages = new ArrayList<Message>();
    }
    
    @Test 
    public void shouldStartTheadOnStart() {
        server.start();
        verify(thread).start();
    }

    @Test(expected=RuntimeException.class)
    public void shouldNotStartTwice() {
        server.start();
        server.start();
    }

    @Test 
    public void shouldInterruptTheadOnStop() {
        server.start();
        server.stop();
        verify(thread).interrupt();
    }

    @Test(expected=RuntimeException.class)
    public void shouldNotStopTwice() {
        server.start();
        server.stop();
        server.stop();
    }

    @Test(expected=RuntimeException.class)
    public void shouldNotStopWhenNotStarted() {
        server.stop();
    }

    @Test
    public void shouldReceiveMessage() throws Exception {
        final Message message = newSampleMessage();
        
        doAnswer(new Answer<Object>(){
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                DatagramPacket packet = (DatagramPacket) invocation.getArguments()[0];
                packet.setData(Json.toBytes(message));
                return null;
            }}).doThrow(new IllegalArgumentException()).when(socket).receive(any(DatagramPacket.class));
        
        server.start();
        try {runnable().run();}
        catch (IllegalArgumentException ignore) {}

        assertEquals(toJson(message), toJson(messages.get(0)));
    }

    private Message newSampleMessage() {
        final UUID uuid = new UUID(123, 456);
        final Iden src = new Iden(Iden.Type.AGT, uuid);
        final Iden dst = new Iden(Iden.Type.CLD, uuid);
        final Message message = new Message(Message.Type.APP, src, dst, "sigval", null);
        return message;
    }
    
    private String toJson(Message message) {
        return Json.toJsonString(message);
    }

    private Runnable runnable() {
        try {
            return runnableCaptor.getValue();
        } catch (Exception any) {
            return null;
        }
    }

    @Override
    public void onMessage(Message message) {
        messages.add(message);
    }
}
