package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.asSet;
import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static com.workshare.msnos.core.CoreHelper.newCloudIden;
import static com.workshare.msnos.core.CoreHelper.synchronousCloudMulticaster;
import static com.workshare.msnos.core.MessagesHelper.newPingMessage;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.cloud.MessagePreProcessors;
import com.workshare.msnos.core.cloud.Multicaster;
import com.workshare.msnos.core.routing.Router;

@SuppressWarnings("unused")
// TODO move here all the receiver test from CloudTest
public class ReceiverTest {
    
    private Router router;
    private Receiver receiver;

    private Cloud cloud;
    private Multicaster caster;

    private Gateway gate;

    @Before
    public void before() {
        gate = mock(Gateway.class);
        LocalAgent local = newLocalAgent(cloud);

        cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(newCloudIden());
        when(cloud.getRing()).thenReturn(Ring.random());
        Internal internal = mock(Cloud.Internal.class);
        when(cloud.internal()).thenReturn(internal);

        router = mock(Router.class);

        Set<Gateway> gates = asSet(gate);
        caster = synchronousCloudMulticaster();
        MessagePreProcessors validators = mock(MessagePreProcessors.class);
        when(validators.isValid(any(Message.class))).thenReturn(new MessagePreProcessors.Result(true, null));

        receiver = new Receiver(cloud, gates, caster, validators , router);
    }

    private LocalAgent newLocalAgent(Cloud clo) {
        LocalAgent local = mock(LocalAgent.class);
        when(local.getIden()).thenReturn(newAgentIden());
        when(local.getCloud()).thenReturn(clo);
        return local;
    }
    
    @Test
    public void shouldInvokeRouterOnReceivedMessage() throws Exception {
        final Message message = newPingMessage(cloud);

        sendMessage(message);
        
        verify(router).process(message);
    }

    private void sendMessage(Message message) {
        ArgumentCaptor<Gateway.Listener> gateListener = ArgumentCaptor.forClass(Gateway.Listener.class);
        verify(gate).addListener(any(Cloud.class), gateListener.capture());
        gateListener.getValue().onMessage(message);
    }
}
