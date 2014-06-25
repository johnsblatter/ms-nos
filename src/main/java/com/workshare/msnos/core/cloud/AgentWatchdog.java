package com.workshare.msnos.core.cloud;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.soup.time.SystemTime;

public class AgentWatchdog {

    private static Logger log = LoggerFactory.getLogger(AgentWatchdog.class);

    private final Cloud cloud;
    private final ScheduledExecutorService scheduler;

    private static final long AGENT_TIMEOUT = Long.getLong("msnos.core.agents.timeout.millis", 60000L);
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
        for (RemoteAgent agent : cloud.getRemoteAgents()) {
            if (agent.getAccessTime() < SystemTime.asMillis() - AGENT_TIMEOUT) {
                log.debug("- sending ping to {}", agent.toString());
                try {
                    cloud.send(new MessageBuilder(Message.Type.PIN, cloud, agent).make());
                } catch (IOException e) {
                    log.debug("Unexpected exception pinging agent " + agent, e);
                }
            }
            if (agent.getAccessTime() < SystemTime.asMillis() - (AGENT_TIMEOUT * AGENT_RETRIES)) {
                log.debug("- remote agent removed due to inactivity: {}", agent);
                cloud.removeFaultyAgent(agent);
            }
        }
        log.trace("Done!");
    }
    
}
