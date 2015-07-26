package com.workshare.msnos.core.cloud;

import static com.workshare.msnos.core.Message.Type.APP;
import static com.workshare.msnos.core.Message.Type.FLT;
import static com.workshare.msnos.core.Message.Type.PIN;
import static com.workshare.msnos.core.Message.Type.PRS;
import static com.workshare.msnos.core.MessagesHelper.*;
import static com.workshare.msnos.core.cloud.CoreHelper.asNetwork;
import static com.workshare.msnos.core.cloud.CoreHelper.asPublicNetwork;
import static com.workshare.msnos.core.cloud.CoreHelper.asSet;
import static com.workshare.msnos.core.cloud.CoreHelper.fakeElapseTime;
import static com.workshare.msnos.core.cloud.CoreHelper.fakeSystemTime;
import static com.workshare.msnos.core.cloud.CoreHelper.getCloudInternal;
import static com.workshare.msnos.core.cloud.CoreHelper.randomUUID;
import static com.workshare.msnos.core.cloud.CoreHelper.synchronousCloudMulticaster;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Gateways;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Identifiable;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.RemoteEntity;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.core.cloud.LocalAgent;
import com.workshare.msnos.core.cloud.Multicaster;
import com.workshare.msnos.core.cloud.Receiver;
import com.workshare.msnos.core.cloud.Sender;
import com.workshare.msnos.core.cloud.Cloud.Internal;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoints;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.receipts.SingleReceipt;
import com.workshare.msnos.core.security.KeysStore;
import com.workshare.msnos.core.security.Signer;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.NTPClient;
import com.workshare.msnos.soup.time.SystemTime;

public class CloudTest {

    private static final String KEY_ID = "123";
    private static final String KEY_VAL = "ABC";

    private static final Iden SOMEONE = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final Iden SOMEONELSE = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final Iden MY_CLOUD = new Iden(Iden.Type.CLD, UUID.randomUUID());

    private HttpGateway httpGate;
    private Set<Gateway> gates;

    private Cloud thisCloud;
    private Cloud otherCloud;
    private Sender sender;

    private ScheduledExecutorService scheduler;
    private List<Message> receivedMessages;
    private KeysStore keystore;
    private Signer signer;
    private File home;
    private Receiver receiver;
    private Multicaster caster;

    @Before
    public void init() throws Exception {
        home = File.createTempFile("msnos-", ".tmp");
        System.setProperty("user.home", home.toString());
        home.delete();
        home.mkdirs();

        sender = mock(Sender.class);
        Receipt receipt = mock(Receipt.class);
        when(sender.send(any(Cloud.class), any(Message.class))).thenReturn(receipt );

        caster = synchronousCloudMulticaster();
        scheduler = mock(ScheduledExecutorService.class);

        Receipt unknownReceipt = mock(Receipt.class);
        when(unknownReceipt.getStatus()).thenReturn(Status.UNKNOWN);

        httpGate = mock(HttpGateway.class);
        when(httpGate.name()).thenReturn("HTTP");
        Endpoints endpoints = mock(Endpoints.class);
        when(httpGate.endpoints()).thenReturn(endpoints);
        when(httpGate.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(unknownReceipt);

        keystore = mock(KeysStore.class);
        signer = new Signer(keystore);

        NTPClient timeClient = mock(NTPClient.class);
        when(timeClient.getTime()).thenReturn(1234L);

        gates = new LinkedHashSet<Gateway>(Arrays.asList(httpGate));
        thisCloud = new Cloud(MY_CLOUD.getUUID(), KEY_ID, signer, sender, receiver, gates, caster, scheduler);
        thisCloud.addListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                receivedMessages.add(message);
            }
        });

        receivedMessages = new ArrayList<Message>();

        otherCloud = new Cloud(UUID.randomUUID(), KEY_ID, signer, sender, receiver, Gateways.NONE, caster, Executors.newSingleThreadScheduledExecutor());
    }

    @After
    public void after() throws Exception {
        SystemTime.reset();
        scheduler.shutdown();
        home.delete();
    }

    @Test
    public void shouldCreateDefaultGateways() throws Exception {
        Set<Gateway> expected = Gateways.all();
        Set<Gateway> current = new Cloud(MY_CLOUD.getUUID()).getGateways();
        assertEquals(expected, current);
    }

    @Test
    public void shouldSendPresenceMessageWhenAgentJoins() throws Exception {
        LocalAgent smith = new LocalAgent(UUID.randomUUID());

        smith.join(thisCloud);

        assertMessageSent(Message.Type.PRS, smith.getIden(), thisCloud.getIden(), null);
    }

    @Test
    public void shouldSendDiscoveryMessageWhenAgentJoins() throws Exception {
        LocalAgent smith = new LocalAgent(UUID.randomUUID());

        smith.join(thisCloud);

        assertMessageSent(Message.Type.DSC, smith.getIden(), thisCloud.getIden(), null);
    }

    @Test
    public void shouldUpdateAgentsListWhenAgentJoins() throws Exception {
        LocalAgent smith = new LocalAgent(UUID.randomUUID());

        smith.join(thisCloud);

        assertTrue(thisCloud.getLocalAgents().contains(smith));
    }

    @Test
    public void shouldDoNothingWhenKnownAgentPongs() throws Exception {
        RemoteAgent remote = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remote, thisCloud);

        simulateMessageFromNetwork(newPongMessage(remote, thisCloud));

        assertNoMessagesSent();
    }

    @Test
    public void shouldSendDiscoveryWhenUnknownAgentPongs() throws Exception {
        RemoteEntity remote = newRemoteAgent(thisCloud);

        simulateMessageFromNetwork(newPongMessage(remote, thisCloud));

        assertMessageSent(Message.Type.DSC, thisCloud.getIden(), remote.getIden(), null);
    }

    @Test
    public void shouldRemoveLocalAgentOnLeave() throws Exception {
        LocalAgent jeff = new LocalAgent(UUID.randomUUID());
        jeff.join(thisCloud);
        jeff.leave();

        assertFalse(thisCloud.getLocalAgents().contains(jeff));
    }

    @Test
    public void shouldRemoveRemoteAgentOnLeave() throws Exception {
        RemoteAgent remote = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remote, thisCloud);

        simulateAgentLeavingCloud(remote, thisCloud);
        assertFalse(thisCloud.getLocalAgents().contains(remote));
    }

    @Test
    public void shouldSendAbsenceWhenLeavingCloudUsingSyncCall() throws Exception {
        LocalAgent karl = new LocalAgent(UUID.randomUUID());
        karl.join(thisCloud);

        karl.leave();

        final Message message = getLastMessageSentSynchronously();
        assertMessageContent(message, PRS, karl.getIden(), thisCloud.getIden(), new Presence(false, karl));
    }

    @Test(expected = Throwable.class)
    public void shouldGetExceptionWhenTryingToLeaveTheCloudWhileNotJoined() throws Exception {
        LocalAgent karl = new LocalAgent(UUID.randomUUID());
        karl.leave();
    }

    @Test
    public void shouldNOTUpdateAgentsListWhenAgentJoinsThroughGatewayToAnotherCloud() throws Exception {
        RemoteAgent frank = newRemoteAgent(otherCloud);

        simulateAgentJoiningCloud(frank, otherCloud);

        assertFalse(thisCloud.getLocalAgents().contains(frank));
    }


    @Test
    public void shouldUpdateRemoteAgentAccessTimeOnPresenceReceived() throws Exception {
        RemoteAgent remoteAgent = newRemoteAgent(thisCloud);

        fakeSystemTime(12345L);
        simulateMessageFromNetwork(newMessage(Message.Type.PRS, remoteAgent.getIden(), thisCloud.getIden()).data(new Presence(true, remoteAgent)));

        fakeElapseTime(100);
        assertEquals(12345L, getRemoteAgentAccessTime(thisCloud, remoteAgent));
    }

    @Test
    public void shouldUpdateRemoteAgentAccessTimeOnMessageReceived() throws Exception {
        RemoteAgent remoteAgent = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remoteAgent, thisCloud);

        fakeSystemTime(99911L);
        final Message message = newPingMessage(remoteAgent, thisCloud);
        simulateMessageFromNetwork(message);

        fakeElapseTime(100);
        assertEquals(99911L, getRemoteAgentAccessTime(thisCloud, remoteAgent));
    }

    @Test
    public void shouldPingAgentsWhenAccessTimeIsTooOld() throws Exception {
        fakeSystemTime(12345L);
        RemoteAgent remoteAgent = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remoteAgent, thisCloud);

        fakeSystemTime(9999999999L);
        forceRunCloudPeriodicCheck();

        Message pingExpected = getLastMessageSent();
        assertNotNull(pingExpected);
        assertEquals(PIN, pingExpected.getType());
        assertEquals(thisCloud.getIden(), pingExpected.getFrom());
    }

    @Test
    public void shouldRemoveAgentsThatDoNOTRespondToPing() throws Exception {
        fakeSystemTime(0L);
        RemoteAgent remoteAgent = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remoteAgent, thisCloud);

        fakeSystemTime(Long.MAX_VALUE);
        forceRunCloudPeriodicCheck();

        assertTrue(!thisCloud.getRemoteAgents().contains(remoteAgent));
    }

    @Test
    public void shouldSendFaultWhenAgentRemoved() throws Exception {
        fakeSystemTime(12345L);
        LocalAgent remoteAgent = new LocalAgent(UUID.randomUUID());
        simulateAgentJoiningCloud(remoteAgent, thisCloud);

        fakeSystemTime(99999999L);
        forceRunCloudPeriodicCheck();

        Message message = getLastMessageSentToCloudListeners();
        assertEquals(FLT, message.getType());
        assertEquals(thisCloud.getIden(), message.getFrom());
        assertEquals(remoteAgent.getIden(), ((FltPayload) message.getData()).getAbout());
    }

    @Test
    public void shouldStoreHostInfoWhenRemoteAgentJoins() throws Exception {
        RemoteAgent remoteFrank = newRemoteAgent(thisCloud);
        Presence presence = (Presence) simulateAgentJoiningCloud(remoteFrank, thisCloud).getData();

        RemoteAgent recordedFrank = getRemoteAgent(thisCloud, remoteFrank.getIden());
        assertEquals(presence.getEndpoints(), recordedFrank.getEndpoints());
    }

    @Test
    public void shouldUpdateRemoteAgentsWhenARemoteJoins() throws Exception {
        RemoteAgent frank = newRemoteAgent(thisCloud);

        simulateAgentJoiningCloud(frank, thisCloud);

        assertEquals(frank.getIden(), thisCloud.getRemoteAgents().iterator().next().getIden());
    }

    @Test
    public void shouldSendSignedMessagesIfKeystoreConfigured() throws Exception {
        when(keystore.get(KEY_ID)).thenReturn(KEY_VAL);

        thisCloud.send(newReliableMessage(APP, SOMEONE, SOMEONELSE));

        Message message = getLastMessageSent();
        assertNotNull(message.getSig());
        assertNotNull(message.getRnd());
    }

    @Test
    public void shouldUpdateHttpGatewayOnRegisterMsnosEndpoints() throws Exception {
        HttpEndpoint endpoint = mock(HttpEndpoint.class);
        when(endpoint.getTarget()).thenReturn(new Iden(Iden.Type.AGT, UUID.randomUUID()));
        thisCloud.registerRemoteMsnosEndpoint(endpoint);
        verify(httpGate.endpoints()).install(endpoint);
    }

    @Test
    public void shouldUpdateRemoteAgentOnRegisterMsnosEndpoints() throws Exception {
        RemoteAgent frank = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(frank, thisCloud);

        HttpEndpoint endpoint = mock(HttpEndpoint.class);
        when(endpoint.getTarget()).thenReturn(frank.getIden());
        thisCloud.registerRemoteMsnosEndpoint(endpoint);

        RemoteAgent agent = thisCloud.getRemoteAgents().iterator().next();
        assertTrue(agent.getEndpoints().contains(endpoint));
    }

    @Test
    public void shouldUpdateLocalAgentOnRegisterLocalMsnosEndpoints() throws Exception {
        LocalAgent smith = new LocalAgent(UUID.randomUUID());
        smith.join(thisCloud);

        HttpEndpoint endpoint = mock(HttpEndpoint.class);
        when(endpoint.getTarget()).thenReturn(smith.getIden());
        thisCloud.registerLocalMsnosEndpoint(endpoint);

        LocalAgent agent = thisCloud.getLocalAgents().iterator().next();
        assertTrue(agent.getEndpoints().contains(endpoint));
    }

    // TODO FIXME
//    public void shouldUpdateHttpGatewayOnUnregisterMsnosEndpoints() throws Exception {

    // TODO FIXME
//    public void shouldUpdateRemoteAgentOnUnregisterMsnosEndpoints() throws Exception {

    // TODO FIXME
//    public void shouldUpdateLocalAgentOnUnregisterLocalMsnosEndpoints() throws Exception {
        
    
    @Test
    public void shouldInstallMsnosEndpointWhenRemoteAgentAdded() throws MsnosException {
        HttpEndpoint endpoint = new HttpEndpoint(asPublicNetwork("25.25.25.25"), "http://foo.com");
        RemoteAgent remote = newRemoteAgent(thisCloud, endpoint);
        
        Internal internal = getCloudInternal(thisCloud);
        internal.remoteAgents().add(remote);
        
        verify(httpGate.endpoints()).install(endpoint);
    }
    
    @Test
    public void shouldUninstallMsnosEndpointWhenRemoteAgentRemoved() throws MsnosException {
        Internal internal = getCloudInternal(thisCloud);
        HttpEndpoint endpoint = new HttpEndpoint(asPublicNetwork("25.25.25.25"), "http://foo.com");
        RemoteAgent remote = newRemoteAgent(thisCloud, endpoint);
        internal.remoteAgents().add(remote);
        
        internal.remoteAgents().remove(remote.getIden());
        
        verify(httpGate.endpoints()).remove(endpoint);
    }
    
    @Test
    public void shouldProcessExternalMessage() throws MsnosException {
        RemoteAgent agent = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(agent, thisCloud);

        Message current = new MessageBuilder(APP, agent, thisCloud).make();
        simulateMessageFromNetwork(current);

        assertEquals(current.getUuid(), getLastMessageSentToCloudListeners().getUuid());
    }

    @Test
    public void shouldEnquiryUponReceivingMessagesFromUnknownAgents() throws Exception {
        RemoteAgent smith = newRemoteAgent(thisCloud);

        simulateMessageFromNetwork(new MessageBuilder(Message.Type.PIN, smith, thisCloud).make());

        Message message = getLastMessageSent();
        assertEquals(Message.Type.DSC, message.getType());
        assertEquals(smith.getIden(), message.getTo());
    }

    @Test
    public void shouldNOTEnquiryUponReceivingMessagesFromCloud() throws Exception {

        simulateMessageFromNetwork(new MessageBuilder(Message.Type.PIN, thisCloud, thisCloud).make());

        assertEquals(0, getAllMessagesSent().size());
    }

    @Test
    public void shouldNOTEnquiryUnknownAgentsMultipleTimes() throws Exception {
        RemoteAgent smith = newRemoteAgent(thisCloud);

        simulateMessageFromNetwork(new MessageBuilder(Message.Type.PIN, smith, thisCloud).make());
        simulateMessageFromNetwork(new MessageBuilder(Message.Type.PIN, smith, thisCloud).make());
        List<Message> messageList = getAllMessagesSent();

        boolean enquired = false;
        for (Message message: messageList) {
            if (Message.Type.DSC == message.getType() && smith.getIden() == message.getTo()) {
                if (!enquired)
                    enquired = true;
                else
                    fail("Multiple subsequent enquiries were sent!");
            }
        }
    }


    @Test
    public void shouldSendMessagesTroughSender() throws Exception {
        Message message = newPingMessage(thisCloud);
        thisCloud.send(message);
        verify(sender).send(thisCloud, message);
    }

    @Test
    public void shouldSendMessagesTroughSenderWhenSync() throws Exception {
        Message message = newPingMessage(thisCloud);
        thisCloud.sendSync(message);
        verify(sender).sendSync(eq(thisCloud), eq(message), any(SingleReceipt.class));
    }

    @Test
    public void shouldAutomaticallyCalculateRing() {
        final Endpoint htp = new HttpEndpoint(asPublicNetwork("25.25.25.25"), "http://25.25.25.25");
        final Endpoint udp = new BaseEndpoint(Endpoint.Type.UDP, asNetwork("192.168.0.199", (short) 16));
        final Set<Endpoint> endpoints = asSet(htp, udp);
        final Gateway gate = mock(Gateway.class);
        when(gate.endpoints()).thenReturn(CoreHelper.makeImmutableEndpoints(endpoints));

        Cloud cloud = new Cloud(randomUUID(), KEY_ID, signer, sender, null, asSet(gate), caster, scheduler);

        assertEquals(Ring.make(endpoints), cloud.getRing());
    }

    @Test
    public void shouldAssingCloudRingToLocalAgentOnJoin() throws Exception {
        LocalAgent smith = new LocalAgent(UUID.randomUUID());
        
        smith.join(thisCloud);
        
        assertEquals(thisCloud.getRing(), smith.getRing());
    }

    @Test
    public void shouldAssingComputedRingToRemoteAgent() throws Exception {
        RemoteAgent frank = newRemoteAgent(thisCloud);
        
        Message message = new MessageBuilder(Message.Type.PRS, frank, thisCloud).with(new Presence(true, frank)).make();
        simulateMessageFromNetwork(message);

        frank = getRemoteAgent(thisCloud, frank.getIden());
        assertNotEquals(thisCloud.getRing(), frank.getRing());
    }

    private RemoteAgent getRemoteAgent(Cloud thisCloud, Iden iden) {
        for (RemoteAgent agent : thisCloud.getRemoteAgents()) {
            if (agent.getIden().equals(iden))
                return agent;
        }
        return null;
    }

    private void forceRunCloudPeriodicCheck() {
        Runnable runnable = capturePeriodicRunableCheck();
        runnable.run();
    }

    private Runnable capturePeriodicRunableCheck() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, atLeastOnce()).scheduleAtFixedRate(captor.capture(), anyInt(), anyInt(), any(TimeUnit.class));
        return captor.getValue();
    }

    private long getRemoteAgentAccessTime(Cloud cloud, RemoteEntity agent) {
        Collection<RemoteAgent> agents = cloud.getRemoteAgents();
        for (RemoteEntity a : agents) {
            if (a.getIden().equals(agent.getIden()))
                return a.getAccessTime();
        }

        throw new RuntimeException("Agent " + agent + " not found!");
    }

    private Message simulateAgentJoiningCloud(Agent agent, Cloud cloud) throws MsnosException {
        Message message = new MessageBuilder(Message.Type.PRS, agent, cloud).with(new Presence(true, agent)).make();
        simulateMessageFromNetwork(message);
        return message;
    }

    private void simulateAgentLeavingCloud(RemoteAgent agent, Cloud cloud) throws MsnosException {
        simulateMessageFromNetwork(new MessageBuilder(PRS, agent.getIden(), cloud.getIden()).reliable(false).with(new Presence(false, agent)).make());
    }

    private void simulateMessageFromNetwork(final Message message) {
        ArgumentCaptor<Gateway.Listener> gateListener = ArgumentCaptor.forClass(Gateway.Listener.class);
        verify(httpGate).addListener(any(Cloud.class), gateListener.capture());
        gateListener.getValue().onMessage(message);
    }

    private Message getLastMessageSentToCloudListeners() {
        final int size = receivedMessages.size();
        if (size > 0)
            return receivedMessages.get(size - 1).fromGate(null);
        else
            return null;
    }

    private List<Message> getAllMessagesSent() throws IOException {
        try {
            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(sender, atLeastOnce()).send(any(Cloud.class), captor.capture());
            return captor.getAllValues();
        } catch (Throwable any) {
            return Collections.emptyList();
        }
    }

    private Message getLastMessageSent() throws IOException {
        List<Message> messageList = getAllMessagesSent();
        return messageList.get(messageList.size() - 1);
    }

    private Message getLastMessageSentSynchronously() throws IOException {
        try {
            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(sender, atLeastOnce()).sendSync(any(Cloud.class), captor.capture(), any(SingleReceipt.class));
            final List<Message> allValues = captor.getAllValues();
            return allValues.get(allValues.size()-1);
        } catch (Throwable any) {
            return mock(Message.class);
        }
    }

    private Message newMessage(final Message.Type type, final Iden idenFrom, final Iden idenTo) {
        return new MessageBuilder(type, idenFrom, idenTo).withHops(1).reliable(false).make();
    }

    private Message newReliableMessage(final Message.Type type, final Iden idenFrom, final Iden idenTo) {
        return new MessageBuilder(type, idenFrom, idenTo).withHops(1).reliable(true).make();
    }

    private void assertNoMessagesSent() throws IOException {
        assertEquals(0, getAllMessagesSent().size());
    }

    private void assertMessageSent(final Type type, final Iden from, final Iden to, Object data) throws IOException {
        List<Message> messageList = getAllMessagesSent();
        for (Message message : messageList) {
            if (message.getType() == type)
                assertMessageContent(message, type, from, to, data);
        }
    }

    private void assertMessageContent(Message message, final Type type, final Iden from, final Iden to, Object data) {
        assertNotNull(message);
        assertEquals(type, message.getType());
        assertEquals(from, message.getFrom());
        assertEquals(to, message.getTo());

        if (data != null)
            assertEquals(Json.toJsonString(data), Json.toJsonString(message.getData()));
    }

    private RemoteAgent newRemoteAgent(Cloud cloud, final Endpoint... endpoints) {
        Set<Endpoint> endpointset = new HashSet<Endpoint>(Arrays.asList(endpoints));
        return new RemoteAgent(UUID.randomUUID(), cloud, endpointset );
    }
}
