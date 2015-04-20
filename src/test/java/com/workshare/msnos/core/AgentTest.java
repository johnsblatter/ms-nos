package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.asSet;
import static com.workshare.msnos.core.CoreHelper.fakeSystemTime;
import static com.workshare.msnos.core.Message.Type.PIN;
import static com.workshare.msnos.core.Message.Type.PON;
import static com.workshare.msnos.core.Message.Type.PRS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.soup.time.SystemTime;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Gateways.class)
public class AgentTest {

    private Cloud cloud;
    private LocalAgent karl;
    private LocalAgent smith;

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(Gateways.class);
        when(Gateways.allPublicEndpoints()).thenReturn(Collections.<Endpoint>emptySet());
        when(Gateways.allEndpoints()).thenReturn(Collections.<Endpoint>emptySet());

        System.setProperty("public.ip", "132.1.0.2");
        cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, UUID.randomUUID()));

        karl = new LocalAgent(UUID.randomUUID());
        karl.join(cloud);

        smith = new LocalAgent(UUID.randomUUID());
        smith.join(cloud);
    }

    @After
    public void after() throws Exception {
        SystemTime.reset();
    }

    @Test
    public void agentShouldAttachListenerToCloud() {
        verify(cloud, atLeastOnce()).addListener(any(Cloud.Listener.class));
    }

    @Test
    public void shouldSendPresenceWhenDiscoveryIsReceived() throws IOException {
        Message discovery = new MessageBuilder(Message.Type.DSC, cloud, smith).make();
        simulateMessageFromCloud(discovery);

        Message message = getLastMessageToCloud();

        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(PRS, message.getType());
    }

    @Test
    public void shouldSendPongWhenPingIsReceived() throws IOException {
        simulateMessageFromCloud(new MessageBuilder(Message.Type.PIN, cloud, smith).make());
        Message message = getLastMessageToCloud();

        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(PON, message.getType());
    }

    @Test
    public void shouldSendUnreliableMessageThroughCloud() throws Exception {
        smith.send(new MessageBuilder(Message.Type.PIN, smith, karl).make());

        Message message = getLastMessageToCloud();
        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(karl.getIden(), message.getTo());
        assertEquals(PIN, message.getType());
        assertEquals(false, message.isReliable());
    }

    @Test
    public void shouldSendReliableMessageThroughCloud() throws Exception {

        smith.send(new MessageBuilder(Message.Type.PIN, smith, karl).reliable(true).make());

        Message message = getLastMessageToCloud();
        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(karl.getIden(), message.getTo());
        assertEquals(PIN, message.getType());
        assertEquals(true, message.isReliable());
    }

    @Test
    public void presenceMessageShouldContainNetworkInfo() throws Exception {
        smith.send(new MessageBuilder(Message.Type.PRS, smith, cloud).with(Presence.on(smith)).make());

        Message message = getLastMessageToCloud();

        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(PRS, message.getType());

        assertNotNull(((Presence) message.getData()).getEndpoints());
        assertEquals(((Presence) message.getData()).getEndpoints(), smith.getEndpoints());
    }

    @Test
    public void localAgentLastAccessTimeShouldAlwaysBeNow() throws Exception {
        fakeSystemTime(123456L);

        LocalAgent jeff = new LocalAgent(UUID.randomUUID());
        jeff.join(cloud);

        assertEquals(jeff.getAccessTime(), SystemTime.asMillis());
    }

    @Test
    public void shouldUpdateAccessTimeWhenMessageIsReceived() {
        fakeSystemTime(123456790L);

        Message message = new MessageBuilder(Message.Type.PIN, cloud.getIden(), smith.getIden()).make();
        simulateMessageFromCloud(message);

        assertEquals(123456790L, smith.getAccessTime());
    }

    @Test
    public void otherAgentsShouldNOTStillSeeAgentOnLeave() throws Exception {
        smith.leave();
        assertFalse(karl.getCloud().getLocalAgents().contains(smith));
    }

    @Test
    public void agentShouldStoreEndpointsInformationAfterJoin() throws Exception {
        final Set<Endpoint> expected = asSet(mock(Endpoint.class));
        when(Gateways.allPublicEndpoints()).thenReturn(expected);
        
        smith.join(cloud);

        assertEquals(expected, smith.getEndpoints());
    }

    @Test
    public void shouldCreateTimestampOnCreation() throws Exception {
        fakeSystemTime(123456L);
        smith.send(new MessageBuilder(Message.Type.PIN, smith, cloud).make());

        Message toCloud = getLastMessageToCloud();
        
        assertEquals(123456L, toCloud.getWhen());
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
}
