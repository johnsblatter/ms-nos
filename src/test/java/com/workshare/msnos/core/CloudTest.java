package com.workshare.msnos.core;

import static com.workshare.msnos.core.Message.Type.APP;
import static com.workshare.msnos.core.Message.Type.PRS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.workshare.msnos.core.Cloud.Multicaster;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.soup.json.Json;

@SuppressWarnings("unchecked")
public class CloudTest {

    private Gateway gate1;
    private Gateway gate2;

    private Cloud thisCloud;
    private Cloud otherCloud;

    private List<Message> messages;

    private static final Iden SOMEONE = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final Iden SOMEONELSE = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final Iden MY_CLOUD = new Iden(Iden.Type.CLD, UUID.randomUUID());

    @Before
    public void init() throws Exception {
        gate1 = Mockito.mock(Gateway.class);
        gate2 = Mockito.mock(Gateway.class);
        thisCloud = new Cloud(MY_CLOUD.getUUID(), new HashSet<Gateway>(Arrays.asList(gate1, gate2)), synchronousMulticaster());
        thisCloud.addListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                messages.add(message);
            }
        });

        messages = new ArrayList<Message>();

        otherCloud = new Cloud(UUID.randomUUID(), Collections.<Gateway>emptySet(), synchronousMulticaster());
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
        Agent smith = new Agent(UUID.randomUUID());

        smith.join(thisCloud);

        List<Message> messageList = getAllMessagesSent();
        assertTrue(!messageList.isEmpty());
        Message message = messageList.get(0);
        assertNotNull(message);
        assertEquals(Message.Type.PRS, message.getType());
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(thisCloud.getIden(), message.getTo());
    }

    @Test
    public void shouldSendDiscoveryMessageWhenAgentJoins() throws Exception {
        Agent smith = new Agent(UUID.randomUUID());

        smith.join(thisCloud);

        List<Message> messageList = getAllMessagesSent();
        Message message = messageList.get(1);
        assertNotNull(message);
        assertEquals(Message.Type.DSC, message.getType());
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(thisCloud.getIden(), message.getTo());
    }

    @Test
    public void shouldUpdateAgentsListWhenAgentJoins() throws Exception {
        Agent smith = new Agent(UUID.randomUUID());

        smith.join(thisCloud);

        assertTrue(thisCloud.getAgents().contains(smith));
    }

    @Test
    public void shouldUpdateAgentsListWhenAgentPongs() throws Exception {
        Agent smith = new Agent(UUID.randomUUID());

        simulateMessageFromNetwork(Messages.pong(smith, thisCloud));

        assertTrue(thisCloud.getAgents().contains(smith));
    }

    @Test
    public void shouldUpdateAgentsListWhenAgentJoinsTroughGateway() throws Exception {
        Agent frank = new Agent(UUID.randomUUID());

        simulateAgentJoiningCloud(frank, thisCloud);

        assertTrue(thisCloud.getAgents().contains(frank));
    }

    @Test
    public void shouldRemoveAgentFromAgentsOnLeave() throws Exception {
        Agent jeff = new Agent(UUID.randomUUID());

        jeff.join(thisCloud);

        assertTrue(thisCloud.getAgents().contains(jeff));

        jeff.leave(thisCloud);

        simulateAgentLeavingCloud(jeff, thisCloud);

        assertFalse(thisCloud.getAgents().contains(jeff));
    }

    @Test
    public void shouldSendAbsenceWhenLeavingCloud() throws Exception {
        Presence data = new Presence(false);

        Agent karl = new Agent(UUID.randomUUID());

        karl.leave(thisCloud);

        Message message = getLastMessageSent();

        assertNotNull(message);
        assertEquals(PRS, message.getType());
        assertEquals(karl.getIden(), message.getFrom());
        assertEquals(Json.toJsonString(data), Json.toJsonString(message.getData()));
    }

    @Test
    public void shouldNOTUpdateAgentsListWhenAgentJoinsTroughGatewayToAnotherCloud() throws Exception {
        Agent frank = new Agent(UUID.randomUUID());

        simulateAgentJoiningCloud(frank, otherCloud);

        assertFalse(thisCloud.getAgents().contains(frank));
    }

    @Test
    public void shouldForwardAnyNonCoreMessageSentToThisCloud() throws Exception {

        simulateMessageFromNetwork(newMessage(APP, SOMEONE, thisCloud.getIden()));
        assertEquals(1, messages.size());
    }

    @Test
    public void shouldForwardAnyNonCoreMessageSentToAnAgentOfTheCloud() throws Exception {
        Agent smith = new Agent(UUID.randomUUID());
        smith.join(thisCloud);

        simulateMessageFromNetwork(newMessage(APP, SOMEONE, smith.getIden()));
        assertEquals(1, messages.size());
    }

    @Test
    public void shouldNOTForwardAnyNonCoreMessageSentToAnAgentOfAnotherCloud() throws Exception {
        Agent smith = new Agent(UUID.randomUUID());
        smith.join(otherCloud);

        simulateMessageFromNetwork(newMessage(APP, SOMEONE, smith.getIden()));
        assertEquals(0, messages.size());
    }

    @Test
    public void shouldForwardAnyNonCoreMessageSentToAnotherCloud() throws Exception {

        simulateMessageFromNetwork(newMessage(APP, SOMEONE, otherCloud.getIden()));
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
    public void shouldSendMessagesReturnUnknownStatusWhenUnreliable() throws Exception {
        Message message = newMessage(APP, SOMEONE, thisCloud.getIden());
        Future<Status> res = thisCloud.send(message);
        assertEquals(UnknownFutureStatus.class, res.getClass());
    }

    @Test
    public void shouldSendMessagesReturnMultipleStatusWhenReliable() throws Exception {
        Future<Status> value1 = createMockFuture(Status.UNKNOWN);
        when(gate1.send(any(Message.class))).thenReturn(value1);

        Future<Status> value2 = createMockFuture(Status.UNKNOWN);
        when(gate2.send(any(Message.class))).thenReturn(value2);

        Message message = newReliableMessage(APP, SOMEONE, SOMEONELSE);
        Future<Status> res = thisCloud.send(message);
        assertEquals(MultipleFutureStatus.class, res.getClass());

        MultipleFutureStatus multi = (MultipleFutureStatus) res;
        assertTrue(multi.statuses().contains(value1));
        assertTrue(multi.statuses().contains(value2));

    }

    private Future<Status> createMockFuture(final Status status) throws InterruptedException, ExecutionException {
        Future<Status> value = Mockito.mock(Future.class);
        when(value.get()).thenReturn(status);
        return value;
    }

    private void simulateAgentJoiningCloud(Agent agent, Cloud cloud) {
        simulateMessageFromNetwork(Messages.presence(agent, cloud));
    }

    private void simulateAgentLeavingCloud(Agent agent, Cloud cloud) {
        simulateMessageFromNetwork(new Message(PRS, agent.getIden(), cloud.getIden(), 2, false, new Presence(false)));
    }

    private void simulateMessageFromNetwork(final Message message) {
        ArgumentCaptor<Listener> gateListener = ArgumentCaptor.forClass(Listener.class);
        verify(gate1).addListener(gateListener.capture());
        gateListener.getValue().onMessage(message);
    }

    private Message getLastMessageSent() throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(gate1).send(captor.capture());
        return captor.getValue();
    }

    private List<Message> getAllMessagesSent() throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(gate1, atLeastOnce()).send(captor.capture());
        return captor.getAllValues();
    }

    private Message newMessage(final Message.Type type, final Iden idenFrom, final Iden idenTo) {
        return new Message(type, idenFrom, idenTo, 1, false, null);
    }

    private Message newReliableMessage(final Message.Type type, final Iden idenFrom, final Iden idenTo) {
        return new Message(type, idenFrom, idenTo, 1, true, null);
    }

    private Multicaster synchronousMulticaster() {
        return new Cloud.Multicaster(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        });
    }

}
