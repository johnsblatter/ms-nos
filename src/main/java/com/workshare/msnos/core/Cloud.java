package com.workshare.msnos.core;

import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Cloud implements Identifiable {

    private static final long AGENT_TIMEOUT = Long.getLong("msnos.core.agents.timeout.millis", 60000L);
    private static final long AGENT_RETRIES = Long.getLong("msnos.core.agents.retries.num", 3);

    private static Logger log = LoggerFactory.getLogger(Cloud.class);

    private static Logger proto = LoggerFactory.getLogger("protocol");

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
    private final Map<Iden, LocalAgent> localAgents;
    private final Map<Iden, RemoteAgent> remoteAgents;

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
        this.localAgents = new HashMap<Iden, LocalAgent>();
        this.remoteAgents = new HashMap<Iden, RemoteAgent>();
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

        final long period = AGENT_TIMEOUT / 2;
        log.debug("Probing agent every {} milliseconds", period);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    probeQuietAgents();
                } catch (IOException e) {
                    log.error("Ping error: {}", e);
                }
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }

    public Iden getIden() {
        return iden;
    }

    public Collection<RemoteAgent> getRemoteAgents() {
        return Collections.unmodifiableCollection(remoteAgents.values());
    }

    public Collection<LocalAgent> getLocalAgents() {
        return Collections.unmodifiableCollection(localAgents.values());
    }

    public Set<Gateway> getGateways() {
        return Collections.unmodifiableSet(gates);
    }

    private void probeQuietAgents() throws IOException {
        log.trace("Probing quite agents...");
        for (RemoteAgent agent : getRemoteAgents()) {
            if (agent.getAccessTime() < SystemTime.asMillis() - AGENT_TIMEOUT) {
                log.debug("- sending ping to {}", agent.toString());
                send(Messages.ping(this, agent));
            }
            if (agent.getAccessTime() < SystemTime.asMillis() - (AGENT_TIMEOUT * AGENT_RETRIES)) {
                log.debug("- remote agent removed due to inactivity: {}", agent);
                localAgents.remove(agent.getIden());
                caster.dispatch(Messages.fault(this, agent));
            }
        }
        log.trace("Done!");
    }

    public Receipt send(Message message) throws IOException {
        proto.info("TX: {} {} {}", message.getType(), message.getFrom(), message.getData());

        MultiGatewayReceipt res = new MultiGatewayReceipt(message);
        for (Gateway gate : gates) {
            res.add(gate.send(message));
        }

        return res;
    }

    void onJoin(LocalAgent agent) throws IOException {
        send(Messages.presence(agent, this));
        send(Messages.discovery(agent, this));
        synchronized (localAgents) {
            log.debug("Local agent joined: {}", agent);
            localAgents.put(agent.getIden(), agent);
        }
    }

    void onLeave(LocalAgent agent) throws IOException {
        send(Messages.absence(agent, this));
        synchronized (localAgents) {
            log.debug("Local agent left: {}", agent);
            localAgents.remove(agent.getIden());
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
        if (isMessageAddressedHere(message)) {
            log.debug("Skipped message sent to somebody else: {}", message);
            return;
        }

        proto.info("RX: {} {} {}", message.getType(), message.getFrom(), message.getData());

        if (isPresence(message)) processPresence(message);
        else if (isAbsence(message)) processAbsence(message);
        else if (isPong(message)) processPong(message);
        else caster.dispatch(message);

        touchSourceAgent(message);
    }

    private boolean isMessageAddressedHere(Message message) {
        return !isLocalCloud(message.getTo()) && !isLocalAgent(message.getTo());
    }

    private boolean isLocalCloud(final Iden to) {
        return iden.equals(to);
    }

    private boolean isLocalAgent(final Iden to) {
        return localAgents.containsKey(to);
    }

    private void processPresence(Message message) {
        Iden from = message.getFrom();
        RemoteAgent agent = new RemoteAgent(from, this).withHosts(((Presence) message.getData()).getNetworks());
        synchronized (remoteAgents) {
            if (!localAgents.containsKey(agent.getIden())) {
                log.debug("Discovered new agent from network: {}", agent.toString());
                remoteAgents.put(agent.getIden(), agent);
            }
        }
    }

    private void processAbsence(Message message) {
        Iden from = message.getFrom();
        RemoteAgent agent = new RemoteAgent(from, this);
        synchronized (remoteAgents) {
            log.debug("Agent from network leaving: {}", agent.toString());
            remoteAgents.remove(agent.getIden());
        }
    }

    private void processPong(Message message) {
        Iden from = message.getFrom();
        RemoteAgent agent = new RemoteAgent(from, this);
        synchronized (remoteAgents) {
            if (!localAgents.containsKey(agent.getIden())) {
                log.debug("Ping from network agent, updating list with: {}", agent.toString());
                remoteAgents.put(agent.getIden(), agent);
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
        if (remoteAgents.containsKey(message.getFrom())) {
            RemoteAgent agent = remoteAgents.get(message.getFrom());
            agent.touch();
        }
    }

}
