package com.workshare.msnos.core;

import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.UUID;

import static com.workshare.msnos.core.Message.Type.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

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

    //   TODO send when not joined to the cloud
    @Test
    public void agentShouldAttachListenerToCloud() {
        verify(cloud, atLeastOnce()).addListener(any(Cloud.Listener.class));
    }

    @Test
    public void shouldSendPresenceWhenDiscoveryIsReceived() throws IOException {
        simulateMessageFromCloud(new Message(DSC, cloud.getIden(), karl.getIden(), 2, false, data()));
        Message message = getLastMessageToCloud();

        assertNotNull(message);
        assertEquals(karl.getIden(), message.getFrom());
        assertEquals(PRS, message.getType());
    }

    @Test
    public void shouldSendUnreliableMessageTroughCloud() throws Exception {
        final JsonObject data = data();

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
        final JsonObject data = data();

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
        verify(cloud, atLeastOnce()).send(captor.capture());
        return captor.getValue();
    }

    private void simulateMessageFromCloud(final Message message) {
        ArgumentCaptor<Cloud.Listener> cloudListener = ArgumentCaptor.forClass(Cloud.Listener.class);
        verify(cloud, atLeastOnce()).addListener(cloudListener.capture());
        cloudListener.getValue().onMessage(message);
    }

    private JsonObject data() {
        return new JsonObject();
    }
}
