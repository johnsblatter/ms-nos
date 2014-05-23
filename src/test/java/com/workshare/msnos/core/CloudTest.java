package com.workshare.msnos.core;

import com.workshare.msnos.core.Cloud.Multicaster;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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

    private static final Iden SOMEONE = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final Iden SOMEONELSE = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final Iden MY_CLOUD = new Iden(Iden.Type.CLD, UUID.randomUUID());

    private Gateway gate1;
    private Gateway gate2;

    private Cloud thisCloud;
    private Cloud otherCloud;

    private ScheduledExecutorService scheduler;
    private List<Message> messages;

    @Before
    public void init() throws Exception {
        scheduler = mock(ScheduledExecutorService.class);

        Receipt unknownReceipt = mock(Receipt.class);
        when(unknownReceipt.getStatus()).thenReturn(Status.UNKNOWN);

        gate1 = mock(Gateway.class);
        when(gate1.send(any(Message.class))).thenReturn(unknownReceipt);
        gate2 = mock(Gateway.class);
        when(gate2.send(any(Message.class))).thenReturn(unknownReceipt);

        thisCloud = new Cloud(MY_CLOUD.getUUID(), new HashSet<Gateway>(Arrays.asList(gate1, gate2)), synchronousMulticaster(), scheduler);
        thisCloud.addListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                messages.add(message);
            }
        });

        messages = new ArrayList<Message>();

        otherCloud = new Cloud(UUID.randomUUID(), Collections.<Gateway>emptySet());
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

        assertMessageSent(Message.Type.PRS,smith.getIden(), thisCloud.getIden(), null);
    }

    @Test
    public void shouldSendDiscoveryMessageWhenAgentJoins() throws Exception {
        LocalAgent smith = new LocalAgent(UUID.randomUUID());       

        smith.join(thisCloud);

        assertMessageSent(Message.Type.DSC,smith.getIden(), thisCloud.getIden(), null);
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
        
        simulateMessageFromNetwork(Messages.pong(remote, thisCloud));

        assertNoMessagesSent();
    }
    
    @Test
    public void shouldSendDiscoveryWhenUnknownAgentPongs() throws Exception {
        RemoteAgent remote = newRemoteAgent(thisCloud);

        simulateMessageFromNetwork(Messages.pong(remote, thisCloud));

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
        assertEquals(1, messages.size());
    }

    @Test
    public void shouldNOTForwardAnyNonCoreMessageSentToAnAgentOfAnotherCloud() throws Exception {
        LocalAgent smith = new LocalAgent(UUID.randomUUID());
        smith.join(otherCloud);

        simulateMessageFromNetwork(newMessage(APP, SOMEONE, smith.getIden()));
        assertEquals(0, messages.size());
    }

    @Test   
    public void shouldNOTForwardAnyNonCoreMessageSentToAnotherCloud() throws Exception {
        simulateMessageFromNetwork(newMessage(APP, SOMEONE, otherCloud.getIden()));
        assertEquals(0, messages.size());
    }

    @Test   
    public void shouldNOTForwardAnyMessageSentFromALocalAgent() throws Exception {
        LocalAgent karl = new LocalAgent(UUID.randomUUID());
        karl.join(thisCloud);
        
        simulateMessageFromNetwork(newMessage(APP, karl.getIden(), thisCloud.getIden()));
        assertEquals(0, messages.size());
    }

    @Test   
    public void shouldNOTForwardAnyMessageSentToARemoteAgent() throws Exception {
        RemoteAgent remote = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remote, thisCloud);
        
        messages.clear();
        simulateMessageFromNetwork(newMessage(APP, thisCloud.getIden(), remote.getIden()));
        assertEquals(0, messages.size());
    }

    @Test
    public void shouldSendMessagesTroughGateways() throws Exception {
        Message message = newMessage(APP, SOMEONE, thisCloud.getIden());
        thisCloud.send(message);
        verify(gate1).send(message);
        verify(gate2).send(message);
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
        when(gate1.send(any(Message.class))).thenReturn(value1);

        Receipt value2 = createMockFuture(Status.UNKNOWN);
        when(gate2.send(any(Message.class))).thenReturn(value2);

        Message message = newReliableMessage(APP, SOMEONE, SOMEONELSE);
        Receipt res = thisCloud.send(message);
        assertEquals(MultiGatewayReceipt.class, res.getClass());

        MultiGatewayReceipt multi = (MultiGatewayReceipt) res;
        assertTrue(multi.getReceipts().contains(value1));
        assertTrue(multi.getReceipts().contains(value2));
    }

    @Test
    public void shouldUpdateRemoteAgentAccessTimeOnPresenceReceived() throws Exception {
        RemoteAgent remoteAgent = newRemoteAgent(thisCloud);

        fakeSystemTime(12345L);
        simulateMessageFromNetwork(Messages.presence(remoteAgent, thisCloud));

        fakeSystemTime(99999L);
        assertEquals(12345L, getRemoteAgentAccessTime(thisCloud, remoteAgent));
    }

    @Test
    public void shouldUpdateRemoteAgentAccessTimeOnMessageReceived() throws Exception {
        RemoteAgent remoteAgent = newRemoteAgent(thisCloud);
        simulateAgentJoiningCloud(remoteAgent, thisCloud);

        fakeSystemTime(12345L);
        simulateMessageFromNetwork(Messages.ping(remoteAgent, thisCloud));

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

        simulateMessageFromNetwork(Messages.app(local, thisCloud, Collections.<String, Object>emptyMap()));

        assertEquals(0, messages.size());
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
        Message message = (Messages.presence(agent, cloud));
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
        return messages.get(messages.size() - 1);
    }

    private Message getLastMessageSentToNetwork() throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(gate1).send(captor.capture());
        return captor.getValue();
    }

    private List<Message> getAllMessagesSent() throws IOException {
        try {
            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(gate1, atLeastOnce()).send(captor.capture());
            return captor.getAllValues();
        }
        catch (Throwable any) {
            return Collections.emptyList();
        }
    }

    private Message getLastMessageSent() throws IOException {
        List<Message> messageList = getAllMessagesSent();
        Message message = messageList.get(messageList.size()-1);
        return message;
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
        return new Cloud.Multicaster(new Executor() {
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
