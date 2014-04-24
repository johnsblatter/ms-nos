package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.payloads.QnePayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class MicroserviceTest {

    Cloud cloud;
    Microservice ms;
    Microservice otherMs;

    @Before
    public void prepare() throws Exception {
        cloud = Mockito.mock(Cloud.class);
        Iden iden = new Iden(Iden.Type.CLD, new UUID(111, 111));
        Mockito.when(cloud.getIden()).thenReturn(iden);

        ms = new Microservice("fluffy");
        otherMs = new Microservice("kiki");

    }

    @Test
    public void shouldInternalAgentJoinTheCloudOnJoin() throws Exception {
        cloud = new Cloud(UUID.randomUUID(), Collections.<Gateway>emptySet());
        ms.join(cloud);
        assertEquals(ms.getAgent(), cloud.getAgents().iterator().next());
    }

    @Test
    public void shouldSendQNEwhenPublishApi() throws Exception {
        ms.join(cloud);

        RestApi api = new RestApi("/foo", 8080);
        ms.publish(api);

        Message msg = getLastMessageSent();
        assertEquals(Message.Type.QNE, msg.getType());
        assertEquals(cloud.getIden(), msg.getTo());
        Set<RestApi> apis = ((QnePayload) msg.getData()).getApis();
        assertEquals(api, apis.iterator().next());
    }

    @Test
    public void shouldSendENQonJoin() throws Exception {
        ms.join(cloud);

        Message msg = getLastMessageSent();

        assertEquals(Message.Type.ENQ, msg.getType());
        assertEquals(cloud.getIden(), msg.getTo());
    }

    @Test
    public void shouldProcessQNEMsgs() throws Exception {
        ms.join(cloud);

        simulateMessageFromCloud(new Message(Message.Type.QNE, cloud.getIden(), ms.getAgent().getIden(), 2, false, new QnePayload(otherMs.getName(), new RestApi("/someApi/otherMS", 222))));

        assertTrue(ms.getMicroServices().containsKey(otherMs.getName()));
    }

    // should send QNE messages after ENQ message

    private Message getLastMessageSent() throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(cloud).send(captor.capture());
        return captor.getValue();
    }

    private void simulateMessageFromCloud(final Message message) {
        ArgumentCaptor<Cloud.Listener> cloudListener = ArgumentCaptor.forClass(Cloud.Listener.class);
        verify(cloud, atLeastOnce()).addListener(cloudListener.capture());
        cloudListener.getValue().onMessage(message);
    }

}
