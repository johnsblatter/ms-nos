package com.workshare.msnos.core.protocols.ip.udp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.protocols.ip.udp.UDPServer;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.Multicaster;

@SuppressWarnings("unchecked")
public class UDPServerTest {

    private UDPServer server;
    private Thread thread;
    private MulticastSocket socket;
    private ArgumentCaptor<Runnable> runnableCaptor;
    private ArgumentCaptor<Message> messageCaptor;
	private Multicaster<Listener, Message> caster;

	@Before
    public void init() {
        thread = Mockito.mock(Thread.class);
        when(thread.isInterrupted()).thenReturn(false);

        ThreadFactory threads = Mockito.mock(ThreadFactory.class);
        runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.when(threads.newThread(runnableCaptor.capture())).thenReturn(thread);
        
        caster = Mockito.mock(Multicaster.class);
        messageCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.doNothing().when(caster).dispatch(messageCaptor.capture());

        socket = Mockito.mock(MulticastSocket.class);
        server = new UDPServer(threads, caster);
    }
    
    @Test 
    public void shouldStartTheadOnStart() {
        server.start(socket, 512);
        verify(thread).start();
    }

    @Test(expected=RuntimeException.class)
    public void shouldNotStartTwice() {
        server.start(socket, 512);
        server.start(socket, 512);
    }

    @Test 
    public void shouldInterruptTheadOnStop() {
        server.start(socket, 512);
        server.stop();
        verify(thread).interrupt();
    }

    @Test(expected=RuntimeException.class)
    public void shouldNotStopTwice() {
        server.start(socket, 512);
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
                packet.setData(server.serializer().toBytes(message));
                return null;
            }}).doThrow(new IllegalArgumentException()).when(socket).receive(any(DatagramPacket.class));
        
        server.start(socket, 512);
        try {runnable().run();}
        catch (IllegalArgumentException ignore) {}

        assertEquals(toJson(message), toJson(getLastMessage()));
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

    private Message getLastMessage() {
    	return messageCaptor.getValue();
    }

    private Message newSampleMessage() {
        final UUID uuid = new UUID(123, 456);
        final Iden src = new Iden(Iden.Type.AGT, uuid);
        final Iden dst = new Iden(Iden.Type.CLD, uuid);
        final Message message = new MessageBuilder(Message.Type.APP, src, dst).make();
        return message;
    }
    
}
