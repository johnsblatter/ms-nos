package com.workshare.msnos.core;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.cloud.JoinSynchronizer;
import com.workshare.msnos.core.cloud.Multicaster;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.security.KeysStore;
import com.workshare.msnos.core.security.Signer;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.workshare.msnos.core.Message.Type.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

public class CloudTest {

    private static final String KEY_ID = "123";
    private static final String KEY_VAL = "ABC";

    private static final Iden SOMEONE = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final Iden SOMEONELSE = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final Iden MY_CLOUD = new Iden(Iden.Type.CLD, UUID.randomUUID());

    private Gateway gate1;
    private Gateway gate2;

    private Cloud thisCloud;
    private Cloud otherCloud;

    private ScheduledExecutorService scheduler;
    private List<Message> receivedMessages;
    private JoinSynchronizer synchro;

    private KeysStore keystore;

    @Before
    public void init() throws Exception {
        scheduler = mock(ScheduledExecutorService.class);

        Receipt unknownReceipt = mock(Receipt.class);
        when(unknownReceipt.getStatus()).thenReturn(Status.UNKNOWN);

        gate1 = mock(Gateway.class);
        when(gate1.send(any(Cloud.class), any(Message.class))).thenReturn(unknownReceipt);
        gate2 = mock(Gateway.class);
        when(gate2.send(any(Cloud.class), any(Message.class))).thenReturn(unknownReceipt);
        synchro = mock(JoinSynchronizer.class);

        keystore = mock(KeysStore.class);
        Signer signer = new Signer(keystore);

        thisCloud = new Cloud(MY_CLOUD.getUUID(), KEY_ID, signer, new HashSet<Gateway>(Arrays.asList(gate1, gate2)), synchro, synchronousMulticaster(), scheduler);
        thisCloud.addListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                receivedMessages.add(message);
            }
        });

        receivedMessages = new ArrayList<Message>();

        otherCloud = new Cloud(UUID.randomUUID(), null, Collections.<Gateway>emptySet(), synchro);
    }

    @After
    public void after() throws Exception {
        SystemTime.reset();
        scheduler.shutdown();
    }

    @Test
    public void shouldCreateDefaultGateways() throws Exception {
        thisCloud = new Cloud(UUID.randomUUID());

        Set<Gateway> gates = thisCloud.getGateways();

        assertEquals(1, gates.size());
        assertEquals(UDPGateway.class, gates.iterator().next().getClass());
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

        simulateMessageFromNetwork(new MockMessageHelper(Message.Type.PON, remote.getIden(), thisCloud.getIden()).make());

        assertNoMessagesSent();
    }

    @Test
    public void shouldSendDiscoveryWhenUnknownAgentPongs() throws Exception {
        RemoteAgent remote = newRemoteAgent(thisCloud);

        simulateMessageFromNetwork(new MockMessageHelper(Message.Type.PON, remote.getIden(), thisCloud.getIden()).make());

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
    public void shouldSendAbsenceWhenLeavingCloud() throws Exception {
        LocalAgent karl = new LocalAgent(UUID.randomUUID());
        karl.join(thisCloud);

        karl.leave();

        assertMessageContent(getLastMessageSent(), PRS, karl.getIden(), thisCloud.getIden(), new Presence(false));
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
    public void shouldForwardAnyNonCoreMessageSentToThisCloud() throws Exception {
        simulateMessageFromNetwork(newMessage(APP, SOMEONE, thisCloud.getIden()));
        assertEquals(1, receivedMessages.size());
    }

    @Test
    public void shouldNOTForwardAnyNonCoreMessageSentToAnAgentOfAnotherCloud() throws Exception {
        RemoteAgent smith = newRemoteAgent(otherCloud);
        simulateMessageFromNetwork(newMessage(APP, SOMEONE, smith.getIden()));
        assertEquals(0, receivedMessages.size());
    }

    @Test
    public void shouldNOTForwardAnyNonCoreMessageSentToAnotherCloud() throws Exception {
        simulateMessageFromNetwork(newMessage(APP, SOMEONE, otherCloud.getIden()));
        assertEquals(0, receivedMessages.size());
    }

    @Test
    public void shouldNOTForwardAnyMessageSentFromALocalAgent() throws Exception {
        LocalAgent karl = new LocalAgent(UUID.randomUUID());
        karl.join(thisCloud);

        simulateMessageFromNetwork(newMessage(APP, karl.getIden(), thisCloud.getIden()));
        assertEquals(0, receivedMessages.size());
    }

    @Test
    public void shouldNOTForwardAnyMessageSentToARemoteAgent() throws Exception {
        RemoteAgent remote = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remote, thisCloud);

        receivedMessages.clear();
        simulateMessageFromNetwork(newMessage(APP, thisCloud.getIden(), remote.getIden()));
        assertEquals(0, receivedMessages.size());
    }

    @Test
    public void shouldSendMessagesTroughGateways() throws Exception {
        Message message = newMessage(APP, SOMEONE, thisCloud.getIden());
        thisCloud.send(message);
        verify(gate1).send(thisCloud, message);
        verify(gate2).send(thisCloud, message);
    }

    @Test
    public void shouldSendReturnUnknownStatusWhenUnreliable() throws Exception {
        Message message = newMessage(APP, SOMEONE, thisCloud.getIden());
        Receipt res = thisCloud.send(message);
        assertEquals(Status.UNKNOWN, res.getStatus());
    }

    @Test
    public void shouldSendReturnMultipleStatusWhenUsingMultipleGateways() throws Exception {
        Receipt value1 = createMockFuture(Status.UNKNOWN);
        when(gate1.send(any(Cloud.class), any(Message.class))).thenReturn(value1);

        Receipt value2 = createMockFuture(Status.UNKNOWN);
        when(gate2.send(any(Cloud.class), any(Message.class))).thenReturn(value2);

        Message message = newReliableMessage(APP, SOMEONE, SOMEONELSE);
        Receipt res = thisCloud.send(message);
        assertEquals(MultiGatewayReceipt.class, res.getClass());

        MultiGatewayReceipt multi = (MultiGatewayReceipt) res;
        assertTrue(multi.getReceipts().contains(value1));
        assertTrue(multi.getReceipts().contains(value2));
    }

    @Test(expected = MsnosException.class)
    public void shouldThrowExceptionWhenSendFailedOnAllGateways() throws Exception {
        when(gate1.send(any(Cloud.class), any(Message.class))).thenThrow(new IOException("boom"));
        when(gate2.send(any(Cloud.class), any(Message.class))).thenThrow(new IOException("boom"));

        Message message = newMessage(APP, SOMEONE, SOMEONELSE);
        thisCloud.send(message);
    }

    @Test
    public void shouldNotThrowExceptionWhenSendFailedOnSomeGateways() throws Exception {
        Receipt value1 = createMockFuture(Status.UNKNOWN);
        when(gate1.send(any(Cloud.class), any(Message.class))).thenReturn(value1);
        when(gate2.send(any(Cloud.class), any(Message.class))).thenThrow(new IOException("boom"));

        Message message = newMessage(APP, SOMEONE, SOMEONELSE);
        Receipt res = thisCloud.send(message);

        MultiGatewayReceipt multi = (MultiGatewayReceipt) res;
        assertTrue(multi.getReceipts().contains(value1));
        assertEquals(1, multi.getReceipts().size());
    }

    @Test
    public void shouldUpdateRemoteAgentAccessTimeOnPresenceReceived() throws Exception {
        RemoteAgent remoteAgent = newRemoteAgent(thisCloud);

        fakeSystemTime(12345L);
        simulateMessageFromNetwork(new MockMessageHelper(Message.Type.PRS, remoteAgent.getIden(), thisCloud.getIden()).data(new Presence(true)).make());

        fakeSystemTime(99999L);
        assertEquals(12345L, getRemoteAgentAccessTime(thisCloud, remoteAgent));
    }

    @Test
    public void shouldUpdateRemoteAgentAccessTimeOnMessageReceived() throws Exception {
        RemoteAgent remoteAgent = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remoteAgent, thisCloud);

        fakeSystemTime(12345L);
        simulateMessageFromNetwork(new MockMessageHelper(Message.Type.PIN, remoteAgent.getIden(), thisCloud.getIden()).make());

        fakeSystemTime(99999L);
        assertEquals(12345L, getRemoteAgentAccessTime(thisCloud, remoteAgent));
    }

    @Test
    public void shouldPingAgentsWhenAccessTimeIsTooOld() throws Exception {
        fakeSystemTime(12345L);
        RemoteAgent remoteAgent = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remoteAgent, thisCloud);

        fakeSystemTime(99999L);
        forceRunCloudPeriodicCheck();

        Message pingExpected = getLastMessageSentToNetwork();
        assertNotNull(pingExpected);
        assertEquals(PIN, pingExpected.getType());
        assertEquals(thisCloud.getIden(), pingExpected.getFrom());
    }

    @Test
    public void shouldRemoveAgentsThatDoNOTRespondToPing() {
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
        assertEquals(presence.getNetworks(), recordedFrank.getHosts());
    }

    @Test
    public void shouldUpdateRemoteAgentsWhenARemoteJoins() throws Exception {
        RemoteAgent frank = newRemoteAgent(thisCloud);

        simulateAgentJoiningCloud(frank, thisCloud);

        assertEquals(frank.getIden(), thisCloud.getRemoteAgents().iterator().next().getIden());
    }

    @Test
    public void shouldSkipMessagesFromLocalAgents() throws Exception {
        LocalAgent local = new LocalAgent(UUID.randomUUID());
        local.join(thisCloud);

        simulateMessageFromNetwork(new MessageBuilder(Message.Type.APP, local, thisCloud).make());

        assertEquals(0, receivedMessages.size());
    }

    @Test
    public void shouldUseSynchronizerOnSuccessfulJoin() throws IOException {
        JoinSynchronizer.Status status = mock(JoinSynchronizer.Status.class);
        when(synchro.start(any(LocalAgent.class))).thenReturn(status);

        LocalAgent local = new LocalAgent(UUID.randomUUID());
        local.join(thisCloud);

        InOrder inOrder = inOrder(synchro);
        inOrder.verify(synchro).start(local);
        inOrder.verify(synchro).wait(status);
        inOrder.verify(synchro).remove(status);
    }

    @Test(expected = IOException.class)
    public void shouldUseSynchronizerOnUnsuccessfulSync() throws IOException {
        JoinSynchronizer.Status status = mock(JoinSynchronizer.Status.class);
        when(synchro.start(any(LocalAgent.class))).thenReturn(status);
        doThrow(IOException.class).when(synchro).wait(status);

        LocalAgent local = new LocalAgent(UUID.randomUUID());
        try {
            local.join(thisCloud);
        } finally {
            InOrder inOrder = inOrder(synchro);
            inOrder.verify(synchro).start(local);
            inOrder.verify(synchro).wait(status);
            inOrder.verify(synchro).remove(status);
        }
    }

    @Test
    public void shouldInvokeSynchronzerWhenMessageReceived() throws Exception {
        RemoteAgent frank = newRemoteAgent(thisCloud);

        Message message = simulateAgentJoiningCloud(frank, thisCloud);

        verify(synchro).process(message);
    }

    @Test
    public void shouldSendSignedMessagesIfKeystoreConfigured() throws Exception {
        when(keystore.get(KEY_ID)).thenReturn(KEY_VAL);

        thisCloud.send(newReliableMessage(APP, SOMEONE, SOMEONELSE));

        Message message = getLastMessageSentToNetwork();
        assertNotNull(message.getSig());
        assertNotNull(message.getRnd());
    }

    @Test
    public void shouldDiscardMessagesSignedWithAnInvalidSignature() throws Exception {
        when(keystore.get(KEY_ID)).thenReturn(KEY_VAL);

        final Message message = newMessage(APP, SOMEONE, thisCloud.getIden()).signed(KEY_ID, "this-is-an-invalid-signature");
        simulateMessageFromNetwork(message);

        assertEquals(0, receivedMessages.size());

    }

    @Test
    public void shouldDiscardMessageFromPast() throws Exception {
        RemoteAgent remoteAgent = mockRemoteWithIden(new Iden(Iden.Type.AGT, UUID.randomUUID()));
        simulateAgentJoiningCloudWithSeq(remoteAgent, 42L);

        Message msg = new MockMessageHelper(APP, SOMEONE, thisCloud.getIden()).sequence(32L).make();
        simulateMessageFromNetwork(msg);

        assertTrue(getAllMessagesSent().isEmpty());
    }

    @Test
    public void shouldUpdateAllRemoteAgentsWhenNewProcessibleMessage() throws Exception {
        RemoteAgent remoteAgent = mockRemoteWithIden(new Iden(Iden.Type.AGT, UUID.randomUUID()));
        simulateAgentJoiningCloudWithSeq(remoteAgent, 42L);

        Message msg = new MockMessageHelper(APP, SOMEONE, thisCloud.getIden()).sequence(52L).make();
        simulateMessageFromNetwork(msg);

        assertEquals(52L, getRemoteAgent(thisCloud, remoteAgent.getIden()).getSeq());
    }


    private void simulateAgentJoiningCloudWithSeq(RemoteAgent remoteAgent, long seq) {
        Message message = (new MockMessageHelper(Type.PRS, remoteAgent.getIden(), thisCloud.getIden()).data(new Presence(true)).sequence(seq).make());
        simulateMessageFromNetwork(message);
    }

    private RemoteAgent mockRemoteWithIden(Iden iden) {
        RemoteAgent remoteAgent = mock(RemoteAgent.class);
        when(remoteAgent.getIden()).thenReturn(iden);
        return remoteAgent;
    }

    private RemoteAgent getRemoteAgent(Cloud thisCloud, Iden iden) {
        for (RemoteAgent agent : thisCloud.getRemoteAgents()) {
            if (agent.getIden().equals(iden)) return agent;
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

    private long getRemoteAgentAccessTime(Cloud cloud, RemoteAgent agent) {
        Collection<RemoteAgent> agents = cloud.getRemoteAgents();
        for (RemoteAgent a : agents) {
            if (a.getIden().equals(agent.getIden()))
                return a.getAccessTime();
        }

        throw new RuntimeException("Agent " + agent + " not found!");
    }

    private Receipt createMockFuture(final Status status) throws InterruptedException, ExecutionException {
        Receipt value = Mockito.mock(Receipt.class);
        when(value.getStatus()).thenReturn(status);
        return value;
    }

    private Message simulateAgentJoiningCloud(Agent agent, Cloud cloud) {
        Message message = (new MockMessageHelper(Message.Type.PRS, agent.getIden(), cloud.getIden()).data(new Presence(true)).make());
        simulateMessageFromNetwork(message);
        return message;
    }

    private void simulateAgentLeavingCloud(RemoteAgent agent, Cloud cloud) {
        simulateMessageFromNetwork(new Message(PRS, agent.getIden(), cloud.getIden(), 2, false, new Presence(false)));
    }

    private void simulateMessageFromNetwork(final Message message) {
        ArgumentCaptor<Listener> gateListener = ArgumentCaptor.forClass(Listener.class);
        verify(gate1).addListener(any(Cloud.class), gateListener.capture());
        gateListener.getValue().onMessage(message);
    }

    private Message getLastMessageSentToCloudListeners() {
        return receivedMessages.get(receivedMessages.size() - 1);
    }

    private Message getLastMessageSentToNetwork() throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(gate1).send(any(Cloud.class), captor.capture());
        return captor.getValue();
    }

    private List<Message> getAllMessagesSent() throws IOException {
        try {
            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(gate1, atLeastOnce()).send(any(Cloud.class), captor.capture());
            return captor.getAllValues();
        } catch (Throwable any) {
            return Collections.emptyList();
        }
    }

    private Message getLastMessageSent() throws IOException {
        List<Message> messageList = getAllMessagesSent();
        return messageList.get(messageList.size() - 1);
    }


    private Message newMessage(final Message.Type type, final Iden idenFrom, final Iden idenTo) {
        return new Message(type, idenFrom, idenTo, 1, false, null);
    }

    private Message newReliableMessage(final Message.Type type, final Iden idenFrom, final Iden idenTo) {
        return new Message(type, idenFrom, idenTo, 1, true, null);
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

    private Multicaster synchronousMulticaster() {
        return new Multicaster(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        });
    }

    private void fakeSystemTime(final long time) {
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return time;
            }
        });
    }

    private RemoteAgent newRemoteAgent(Cloud cloud) {
        return new RemoteAgent(UUID.randomUUID(), cloud, Collections.<Network>emptySet());
    }

}
