package com.workshare.msnos.core;

import static com.workshare.msnos.core.Message.Type.APP;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.JsonObject;

public class AgentTest {

    private Cloud cloud;
    private Agent karl;
    private Agent smith;

    @Before
    public void before() throws Exception {
        cloud = mock(Cloud.class);

        smith = new Agent(UUID.randomUUID());
        smith.join(cloud);

        karl = new Agent(UUID.randomUUID());
        karl.join(cloud);
    }
    
//    send when not joined to the cloud

    @Test
    public void shouldSendUnreliableMessageTroughCloud() throws Exception {
        final JsonObject data = new JsonObject();
        smith.sendMessage(karl, APP, data);
        
        Message message = getLastMessageToCloud();
        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(karl.getIden(), message.getTo());
        assertEquals(APP, message.getType());
        assertEquals(data, message.getData());
        assertEquals(false, message.isReliable());
    }

    @Test
    public void shouldSendReliableMessageTroughCloud() throws Exception {
        final JsonObject data = new JsonObject();
        smith.sendReliableMessage(karl, APP, data);
        
        Message message = getLastMessageToCloud();
        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(karl.getIden(), message.getTo());
        assertEquals(APP, message.getType());
        assertEquals(data, message.getData());
        assertEquals(true, message.isReliable());
    }

    private Message getLastMessageToCloud() throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(cloud).send(captor.capture());
        return captor.getValue();
    }
}
