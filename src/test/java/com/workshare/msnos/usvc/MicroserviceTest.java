package com.workshare.msnos.usvc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;

public class MicroserviceTest {

    Cloud cloud;
    Microservice ms;

    @Before
    public void prepare() throws Exception {
        cloud = Mockito.mock(Cloud.class);
        Iden iden = new Iden(Iden.Type.CLD, new UUID(111,111));
        Mockito.when(cloud.getIden()).thenReturn(iden);
        
        ms = new Microservice("fluffy");
    }

    @Test
    public void shouldInternalAgentJoinTheCloudOnJoin() throws Exception {
        cloud = new Cloud(UUID.randomUUID(), Collections.<Gateway>emptySet() );
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
    }

    // should process QNE messages
    // should send QNE messages aftee ENQ message

    private Message getLastMessageSent() throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(cloud).send(captor.capture());
        return captor.getValue();
    }
}
