package com.workshare.msnos.integration;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.LocalAgent;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class CoreIT {

    private LocalAgent agent;
    private Cloud cloud;

    @Before
    public void setUp() throws Exception {
        agent = new LocalAgent(UUID.randomUUID());
        cloud = new Cloud(new UUID(111, 222));
        agent.join(cloud);
    }

    @Test
    public void shouldSeeRemoteOnJoin() throws Exception {
        Thread.sleep(100);
        assertTrue(cloud.getRemoteAgents().size() == 1);
    }
}
