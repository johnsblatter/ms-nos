package com.workshare.msnos.core;

import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import static com.workshare.msnos.core.Message.Type.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AgentTest {

    private static Logger log = LoggerFactory.getLogger(Agent.class);

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
        simulateMessageFromCloud(Messages.discovery(cloud, karl));
        Message message = getLastMessageToCloud();

        assertNotNull(message);
        assertEquals(karl.getIden(), message.getFrom());
        assertEquals(PRS, message.getType());
    }

    @Test
    public void shouldSendPongWhenPingIsReceived() throws IOException {
        simulateMessageFromCloud(Messages.ping(cloud, karl));
        Message message = getLastMessageToCloud();

        assertNotNull(message);
        assertEquals(karl.getIden(), message.getFrom());
        assertEquals(PON, message.getType());
    }

    @Test
    public void shouldSendUnreliableMessageThroughCloud() throws Exception {
        final JsonObject data = data();

        smith.send(Messages.app(smith, karl, data));

        Message message = getLastMessageToCloud();
        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(karl.getIden(), message.getTo());
        assertEquals(APP, message.getType());
        assertEquals(data, message.getData());
        assertEquals(false, message.isReliable());
    }

    @Test
    public void shouldSendReliableMessageThroughCloud() throws Exception {
        final JsonObject data = data();

        smith.send(Messages.app(smith, karl, data).reliable());

        Message message = getLastMessageToCloud();
        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(karl.getIden(), message.getTo());
        assertEquals(APP, message.getType());
        assertEquals(data, message.getData());
        assertEquals(true, message.isReliable());
    }

    @Test
    public void otherAgentsShouldNOTStillSeeAgentOnLeave() throws Exception {
        smith.leave(cloud);
        assertFalse(karl.getCloud().getAgents().contains(smith));
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
