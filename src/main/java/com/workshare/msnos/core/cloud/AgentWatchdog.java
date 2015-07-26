package com.workshare.msnos.core.cloud;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.RemoteEntity;
import com.workshare.msnos.soup.time.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentWatchdog {

    private static Logger log = LoggerFactory.getLogger(AgentWatchdog.class);

    private final Cloud cloud;
    private final ScheduledExecutorService scheduler;

    private static final long AGENT_TIMEOUT = Long.getLong("msnos.core.agents.timeout.millis", 90000L);
    private static final long AGENT_RETRIES = Long.getLong("msnos.core.agents.retries.num", 3);

    public AgentWatchdog(Cloud cloud, ScheduledExecutorService executor) {
        this.cloud = cloud;
        this.scheduler = executor;
    }

    public void start() {
        final long period = AGENT_TIMEOUT / 2;
        log.debug("Probing agent every {} milliseconds", period);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                probeQuietAgents();
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }

    private void probeQuietAgents() {
        log.trace("Probing quite agents...");
        for (RemoteEntity agent : cloud.getRemoteAgents()) {
            final long currentTime = SystemTime.asMillis();
            final long agentTime = agent.getAccessTime();
            if (agentTime < currentTime - AGENT_TIMEOUT) {
                log.debug("- sending ping to "+uuidOf(agent)+" - agentTime {}, currentTime {}", agentTime, currentTime);
                try {
                    cloud.send(new MessageBuilder(Message.Type.PIN, cloud, agent).make());
                } catch (IOException e) {
                    log.debug("Unexpected exception pinging agent " + agent, e);
                }
            }
            if (agentTime < currentTime - (AGENT_TIMEOUT * AGENT_RETRIES)) {
                log.debug("- remote agent {} removed due to inactivity: {}", uuidOf(agent), agent);
                cloud.removeFaultyAgent(agent);
            }
        }
        log.trace("Done!");
    }

    private UUID uuidOf(RemoteEntity agent) {
        return agent.getIden().getUUID();
    }

}
