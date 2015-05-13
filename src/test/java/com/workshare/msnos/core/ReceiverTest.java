package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.*;
import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static com.workshare.msnos.core.CoreHelper.newCloudIden;
import static com.workshare.msnos.core.CoreHelper.synchronousCloudMulticaster;
import static com.workshare.msnos.core.Message.Type.APP;
import static com.workshare.msnos.core.MessagesHelper.newPingMessage;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.cloud.IdentifiablesList;
import com.workshare.msnos.core.cloud.MessageValidators;
import com.workshare.msnos.core.cloud.Multicaster;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.routing.Router;
import com.workshare.msnos.core.security.KeysStore;
import com.workshare.msnos.core.security.Signer;
import com.workshare.msnos.soup.time.SystemTime;

@SuppressWarnings("unused")
// TODO move here all the receiver test from CloudTest
public class ReceiverTest {
    
    private Router router;
    private Receiver receiver;

    private Cloud cloud;
    private Multicaster caster;

    private Gateway gate;
    private LocalAgent local;

    @Before
    public void before() {
        cloud = createMockCloud();

        gate = mock(Gateway.class);
        local = newLocalAgent();
        
        router = mock(Router.class);

        Set<Gateway> gates = asSet(gate);
        caster = mock(Multicaster.class);
        MessageValidators validators = new MessageValidators(cloud.internal());

        receiver = new Receiver(cloud, gates, caster, router);
    }

    @After
    public void after() throws Exception {
        SystemTime.reset();
    }

    @Test
    public void shouldInvokeRouterOnReceivedMessage() throws Exception {

        final Message message = newPingMessage(cloud);
        simulateMessageReceived(message);
        
        verify(router).forward(message);
    }

    @Test
    public void shouldInvokeCasterOnReceivedMessage() throws Exception {

        final Message message = newPingMessage(cloud);
        simulateMessageReceived(message);
        
        verify(caster).dispatch(message);
    }

    @Test
    public void shouldInvokeCloudProcessOnReceivedMessage() throws Exception {

        final Message message = newPingMessage(cloud);
        simulateMessageReceived(message);
        
        verify(cloud).postProcess(message);
    }

    @Test
    public void shouldStampMessageWithGateName() throws Exception {

        when(gate.name()).thenReturn("YOP");
        
        final Message message = newPingMessage(cloud);
        simulateMessageReceived(message);

        assertEquals("YOP", lastMessageReceived().getReceivingGate());
    }

    @Test
    public void shouldDiscardMessageFromAnotherCloud() throws Exception {
        Cloud other = createMockCloud();

        final Message message = newPingMessage(other);
        simulateMessageReceived(message);

        verifyMessageDiscarded();
    }

    @Test
    public void shouldDiscardMessageSentFromLocalAgent() throws Exception {

        final Message message = newPingMessage(local, cloud);
        simulateMessageReceived(message);
        
        verifyMessageDiscarded();
    }

    @Test
    public void shouldNOTInvokeCloudOnMessageSentToRemoteAgent() throws Exception {
        RemoteAgent remote = newRemoteAgent(cloud);

        final Message message = newPingMessage(cloud, remote);
        simulateMessageReceived(message);

        verify(cloud, never()).postProcess(message);
    }

    @Test
    public void shouldNOTInvokeCasterOnMessageSentToARemoteAgent() throws Exception {
        RemoteAgent remote = newRemoteAgent(cloud);

        final Message message = newPingMessage(cloud, remote);
        simulateMessageReceived(message);

        verifyZeroInteractions(caster);
    }
    
    @Test
    public void shouldALWAYSInvokeRouterOnMessageSentToARemoteAgent() throws Exception {
        RemoteAgent remote = newRemoteAgent(cloud);

        final Message message = newPingMessage(cloud, remote);
        simulateMessageReceived(message);

        verify(router).forward(message);
    }
    
    @Test
    public void shouldDiscardMessagesSignedWithAnInvalidSignature() throws Exception {
        mockMessageSigning("sign-key", "correct-signature");
        
        final Message message = newPingMessage(cloud, local).signed("sign-key", "this-is-an-invalid-signature");
        simulateMessageReceived(message);

        verifyMessageDiscarded();
    }

    @Test
    public void shouldDiscardMessagesAlreadyReceived() throws Exception {
        final Message message = newPingMessage(cloud, newRemoteAgent(cloud));
        simulateMessageReceived(message);
        reset(caster, router);
        
        simulateMessageReceived(message);
        
        verifyZeroInteractions(caster, router);
    }

    @Test
    public void shouldDiscardMessagesWithOldTimestamps() throws Exception {
        fakeSystemTime(100000);
        Message message = newPingMessage(cloud, newRemoteAgent(cloud));

        fakeSystemTime(9900000);
        simulateMessageReceived(message);

        verifyMessageDiscarded();
    }


    private void verifyMessageDiscarded() {
        verifyZeroInteractions(caster, router);
        verify(cloud, never()).postProcess(any(Message.class));
    }

    private void simulateMessageReceived(Message message) {
        ArgumentCaptor<Gateway.Listener> gateListener = ArgumentCaptor.forClass(Gateway.Listener.class);
        verify(gate).addListener(any(Cloud.class), gateListener.capture());
        gateListener.getValue().onMessage(message);
    }
    
    private LocalAgent newLocalAgent() {
        final LocalAgent local = mock(LocalAgent.class);
        when(local.getIden()).thenReturn(newAgentIden());
        when(local.getCloud()).thenReturn(cloud);
        cloud.internal().localAgents().add(local);
        return local;
    }
    
    private RemoteAgent newRemoteAgent(Cloud cloud) {
        final RemoteAgent remote = new RemoteAgent(UUID.randomUUID(), cloud, Collections.<Endpoint> emptySet());
        cloud.internal().remoteAgents().add(remote);
        return remote;
    }

    private void mockMessageSigning(final String signKey, final String signVal) {
        when(cloud.internal().sign(any(Message.class))).thenAnswer(new Answer<Message>(){
            @Override
            public Message answer(InvocationOnMock invocation) throws Throwable {
                final Message message = (Message) invocation.getArguments()[0];
                return message.signed(signKey, signVal);
            }});
    }
    
    private Message lastMessageReceived() {
        ArgumentCaptor<Message> received = ArgumentCaptor.forClass(Message.class);
        verify(router).forward(received.capture());
        final Message lastReceived = received.getValue();
        return lastReceived;
    }
}
