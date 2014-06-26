package com.workshare.msnos.integration;

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;

public class CoreXXX  implements IntegrationTest {

    private LocalAgent agent;
    private Cloud cloud;

    @Before
    public void setUp() throws Exception {
        agent = new LocalAgent(UUID.randomUUID());
        cloud = new Cloud(CLOUD_UUID);
        agent.join(cloud);
    }

    @Test
    public void shouldSeeRemoteOnJoin() throws Exception {
        Thread.sleep(100);
        assertTrue(cloud.getRemoteAgents().size() == 1);
    }

    public void run() throws MsnosException {
        Payload data = null; 
        Message message = new MessageBuilder(Type.APP, agent, cloud).with(data).make();
        cloud.send(message);
    }
    
    public static void main(String[] args) throws Exception {
        CoreXXX test = new CoreXXX();
        test.setUp();
        test.run();
    }
}
