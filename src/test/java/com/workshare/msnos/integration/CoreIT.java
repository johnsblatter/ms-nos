package com.workshare.msnos.integration;

import static com.workshare.msnos.integration.IntegrationActor.CommandPayload.Command.AGENT_JOIN;
import static com.workshare.msnos.integration.IntegrationActor.CommandPayload.Command.AGENT_LEAVE;
import static com.workshare.msnos.integration.IntegrationActor.CommandPayload.Command.SELF_KILL;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.integration.IntegrationActor.CommandPayload.Command;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CoreIT implements IntegrationActor {

    static {
        // do not use NTP in integration tests
        System.setProperty("com.ws.nsnos.time.local", "true");
    }

    private static LocalAgent masterAgent;
    private static Cloud masterCloud;
    
    private static LocalAgent testAgent;
    private static Cloud testCloud;
    
    protected void sendCommand(final Command command) throws MsnosException {
        Payload data = CommandPayload.create(command); 
        Message message = new MessageBuilder(Type.APP, masterAgent, masterCloud).with(data).make();
        masterCloud.send(message);
    }

    public static interface Condition {
        public boolean verified();
    }

    private final Condition ONE_AGENT_IN_CLOUD = new Condition() {
        @Override
        public boolean verified() {
            return testCloud.getRemoteAgents().size() == 1;
        }};

    private final Condition NO_AGENTS_IN_CLOUD = new Condition() {
        @Override
        public boolean verified() {
            return testCloud.getRemoteAgents().size() == 0;
        }
    };
        
    @BeforeClass
    public static void bootstrap() throws MsnosException {
        System.setProperty("msnos.core.agents.timeout.millis", "500");
        
        masterAgent = new LocalAgent(UUID.randomUUID());
        masterCloud = new Cloud(MASTER_CLOUD_UUID);
        masterAgent.join(masterCloud);
    
        testCloud = new Cloud(GENERIC_CLOUD_UUID);
        testAgent = new LocalAgent(UUID.randomUUID());
        testAgent.join(testCloud);
    }

    @After 
    public void teardown() throws Exception {
        sendCommand(AGENT_LEAVE);
        waitForCondition(NO_AGENTS_IN_CLOUD);
    }
    
    @Test
    public void s01_shouldSeeRemoteJoining() throws Exception {
        sendCommand(AGENT_JOIN);
        assertCondition(ONE_AGENT_IN_CLOUD);
    }

    @Test
    public void s02_shouldUnseeRemoteLeaving() throws Exception {
        sendCommand(AGENT_JOIN);
        assertCondition(ONE_AGENT_IN_CLOUD);

        sendCommand(AGENT_LEAVE);
        assertCondition(NO_AGENTS_IN_CLOUD);
    }

    @Test
    public void s03_shouldUnseeRemoteDying() throws Exception {
        sendCommand(AGENT_JOIN);
        assertCondition(ONE_AGENT_IN_CLOUD);

        sendCommand(SELF_KILL);
        assertCondition(NO_AGENTS_IN_CLOUD);
    }


    private void assertCondition(final Condition condition)  {
        assertTrue(waitForCondition(condition, 5000L));
    }

    private boolean waitForCondition(Condition condition) {
        return waitForCondition(condition, 5000L);
    }

    private boolean waitForCondition(Condition condition, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        while(System.currentTimeMillis() < end) {
            if (condition.verified())
                return true;

            sleep(250L);
        }
        
        return false;
    }

    private void sleep(final long amount) {
        try {
            Thread.sleep(amount);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
}
