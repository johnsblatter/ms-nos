package com.workshare.msnos.core;

import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class Cloud implements Identifiable {

    private static final long AGENT_TIMEOUT = Long.getLong("msnos.core.agents.timeout", 60000L);
    private static final long AGENT_RETRIES = Long.getLong("msnos.core.agents.retries", 3);

    private static Logger log = LoggerFactory.getLogger(Cloud.class);

    public static interface Listener {
        public void onMessage(Message message);
    }

    public static class Multicaster extends com.workshare.msnos.soup.threading.Multicaster<Listener, Message> {
        public Multicaster() {
            super();
        }

        public Multicaster(Executor executor) {
            super(executor);
        }

        @Override
        protected void dispatch(Listener listener, Message message) {
            listener.onMessage(message);
        }
    }

    private final Iden iden;
    private final Map<Iden, Agent> agents;

    transient private final Set<Gateway> gates;
    transient private final Multicaster caster;
    transient private final ScheduledExecutorService scheduler;

    public Cloud(UUID uuid) throws IOException {
        this(uuid, Gateways.all());
    }

    public Cloud(UUID uuid, Set<Gateway> gates) throws IOException {
        this(uuid, gates, new Multicaster(), Executors.newSingleThreadScheduledExecutor());
    }

    public Cloud(UUID uuid, Set<Gateway> gates, Multicaster multicaster, ScheduledExecutorService executor) {
        this.iden = new Iden(Iden.Type.CLD, uuid);
        this.agents = new HashMap<Iden, Agent>();
        this.caster = multicaster;
        this.gates = gates;
        this.scheduler = executor;

        for (Gateway gate : gates) {
            gate.addListener(new Gateway.Listener() {
                @Override
                public void onMessage(Message message) {
                    process(message);
                }
            });
        }

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    probeQuietAgents();
                } catch (IOException e) {
                    log.error("Ping error: {}", e);
                }
            }
        }, 10, TimeUnit.SECONDS);
    }

    public Iden getIden() {
        return iden;
    }

    public Collection<Agent> getAgents() {
        return Collections.unmodifiableCollection(agents.values());
    }

    public Set<Gateway> getGateways() {
        return Collections.unmodifiableSet(gates);
    }

    private void probeQuietAgents() throws IOException {
        log.debug("Probing quite agents...");
        for (Agent agent : getAgents()) {
            if (agent.getAccessTime() < SystemTime.asMillis() - AGENT_TIMEOUT) {
                log.debug("- sending ping to {}", agent.toString());
                send(Messages.ping(this, agent));
            }
            if (agent.getAccessTime() < SystemTime.asMillis() - (AGENT_TIMEOUT * AGENT_RETRIES)) {
                log.debug("- remote agent removed due to inactivity: {}", agent);
                agents.remove(agent.getIden());
            }
        }
        log.debug("Done!");
    }

    public Future<Message.Status> send(Message message) throws IOException {
        CompositeFutureStatus res;
        if (!message.isReliable())
            res = new UnknownFutureStatus();
        else
            res = new MultipleFutureStatus();

        for (Gateway gate : gates) {
            res.add(gate.send(message));
        }

        return res;
    }

    void onLeave(Agent agent) throws IOException {
        send(Messages.absence(agent, this));
        synchronized (agents) {
            log.debug("Local agent left: {}", agent);
            agents.remove(agent.getIden());
        }
    }

    void onJoin(Agent agent) throws IOException {
        send(Messages.presence(agent, this));
        send(Messages.discovery(agent, this));
        synchronized (agents) {
            log.debug("Local agent joined: {}", agent);
            agents.put(agent.getIden(), agent);
        }
    }

    public Listener addListener(com.workshare.msnos.core.Cloud.Listener listener) {
        return caster.addListener(listener);
    }

    void removeListener(com.workshare.msnos.core.Cloud.Listener listener) {
        log.debug("Removing listener: {}", listener);
        caster.removeListener(listener);
    }

    protected void process(Message message) {
        if (!this.iden.equals(message.getTo()) && !agents.containsKey(message.getTo())) {
            log.debug("Skipped message sent to another cloud: {}", message);
            return;
        }

        if (isPresence(message)) processPresence(message);
        else if (isAbsence(message)) processAbsence(message);
        else if (isPong(message)) processPong(message);
        else caster.dispatch(message);

        touchSourceAgent(message);
    }

    private void processPresence(Message message) {
        Iden from = message.getFrom();
        Agent agent = new Agent(from, this);
        synchronized (agents) {
            if (!agents.containsKey(agent.getIden())) {
                log.debug("Discovered new agent from network: {}", agent.toString());
                agents.put(agent.getIden(), agent);
            }
        }
    }

    private void processAbsence(Message message) {
        Iden from = message.getFrom();
        Agent agent = new Agent(from, this);
        synchronized (agents) {
            log.debug("Agent from network leaving: {}", agent.toString());
            agents.remove(agent.getIden());
        }
    }

    private void processPong(Message message) {
        Iden from = message.getFrom();
        Agent agent = new Agent(from, this);
        synchronized (agents) {
            if (!agents.containsKey(agent.getIden())) {
                log.debug("Ping from network agent, updating list with: {}", agent.toString());
                agents.put(agent.getIden(), agent);
            }
        }
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    private boolean isAbsence(Message message) {
        return message.getType() == Message.Type.PRS && !((Presence) message.getData()).isPresent();
    }

    private boolean isPresence(Message message) {
        return message.getType() == Message.Type.PRS && ((Presence) message.getData()).isPresent();
    }

    private boolean isPong(Message message) {
        return message.getType() == Message.Type.PON;
    }

    private void touchSourceAgent(Message message) {
        if (agents.containsKey(message.getFrom())) {
            Agent agent = agents.get(message.getFrom());
            agent.touch();
        }
    }

}
