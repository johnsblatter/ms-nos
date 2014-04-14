package com.workshare.msnos.core;

import com.workshare.msnos.soup.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class Cloud implements Identifiable {

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

    public Cloud(UUID uuid) throws IOException {
        this(uuid, Gateways.all(), new Multicaster());
    }

    public Cloud(UUID uuid, Set<Gateway> gates) throws IOException {
        this(uuid, gates, new Multicaster());
    }

    public Cloud(UUID uuid, Set<Gateway> gates, Multicaster multicaster) throws IOException {
        this.iden = new Iden(Iden.Type.CLD, uuid);
        this.agents = new HashMap<Iden, Agent>();
        this.caster = multicaster;

        this.gates = gates;
        for (Gateway gate : gates) {
            gate.addListener(new Gateway.Listener() {
                @Override
                public void onMessage(Message message) {
                    process(message);
                }
            });
        }
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

    Future<Message.Status> send(Message message) throws IOException {
        CompositeFutureStatus res = null;
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
            log.debug("Local agent joined: {}", agent);
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
        return message.getType() == Message.Type.PRS && message.getData().equals(Messages.STATUS_FALSE);
    }

    private boolean isPresence(Message message) {
        return message.getType() == Message.Type.PRS && message.getData().equals(Messages.STATUS_TRUE);
    }

    private boolean isPong(Message message) {
        return message.getType() == Message.Type.PON;
    }

}
