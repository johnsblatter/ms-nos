package com.workshare.msnos.core;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.cloud.AgentWatchdog;
import com.workshare.msnos.core.cloud.IdentifiablesList;
import com.workshare.msnos.core.cloud.JoinSynchronizer;
import com.workshare.msnos.core.cloud.JoinSynchronizer.Status;
import com.workshare.msnos.core.cloud.MessagePreProcessors;
import com.workshare.msnos.core.cloud.Multicaster;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.security.Signer;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.soup.time.SystemTime;

public class Cloud implements Identifiable {

    private static final ScheduledExecutorService DEFAULT_SCHEDULER = ExecutorServices.newSingleThreadScheduledExecutor();
    
    private static final Logger log = LoggerFactory.getLogger(Cloud.class);
    private static final Logger proto = LoggerFactory.getLogger("protocol");

    private static final SecureRandom random = new SecureRandom();

    public static interface Listener {
        public void onMessage(Message message);
    }

    private final Iden iden;
    private final String signid;
    private final IdentifiablesList<LocalAgent> localAgents;
    private final IdentifiablesList<RemoteAgent> remoteAgents;
    private final IdentifiablesList<RemoteEntity> remoteClouds;
    private final AtomicLong seq;

    transient private final MessagePreProcessors validators;
    transient private final Set<Gateway> gates;
    transient private final Multicaster caster;
    transient private final JoinSynchronizer synchronizer;
    transient private final Signer signer;
    transient private final Internal internal;

    public class Internal {
        public IdentifiablesList<LocalAgent> localAgents() {
            return localAgents;
        }

        public IdentifiablesList<RemoteAgent> remoteAgents() {
            return remoteAgents;
        }

        public IdentifiablesList<RemoteEntity> remoteClouds() {
            return remoteClouds;
        }

        public Message sign(Message message) {
            return Cloud.this.sign(message);
        }

        public Cloud cloud() {
            return Cloud.this;
        }
    }

    public Cloud(UUID uuid) throws MsnosException {
        this(uuid, null);
    }

    public Cloud(UUID uuid, String signid) throws MsnosException {
        this(uuid, signid, Gateways.all(), new JoinSynchronizer(), random.nextLong());
    }

    public Cloud(UUID uuid, String signid, Set<Gateway> gates, JoinSynchronizer synchronizer,  Long instanceId) {
        this(uuid, signid, new Signer(), gates, synchronizer, new Multicaster(), DEFAULT_SCHEDULER, instanceId);
    }

    Cloud(UUID uuid, String signid, Signer signer, Set<Gateway> gates, JoinSynchronizer synchronizer, Multicaster multicaster, ScheduledExecutorService executor, Long instanceId) {
        this.iden = new Iden(Iden.Type.CLD, uuid, instanceId);

        this.localAgents = new IdentifiablesList<LocalAgent>();
        this.remoteAgents = new IdentifiablesList<RemoteAgent>();
        this.remoteClouds = new IdentifiablesList<RemoteEntity>(); 

        this.seq = new AtomicLong(SystemTime.asMillis());
        this.caster = multicaster;
        this.gates = gates;
        this.signer = signer;
        this.signid = signid;
        this.synchronizer = synchronizer;

        for (Gateway gate : gates) {
            gate.addListener(this, new Gateway.Listener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        process(message);
                    } catch (IOException e) {
                        log.error("Cannot process message", e);
                    }
                }
            });
        }
        
        this.internal = new Internal();
        this.validators = new MessagePreProcessors(this.internal);
        
        new AgentWatchdog(this, executor).start();
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
    public Iden getIden() {
        return iden;
    }

    public Long getNextSequence() {
        return seq.incrementAndGet();
    }

    public Collection<RemoteAgent> getRemoteAgents() {
        return remoteAgents.list();
    }

    public Collection<LocalAgent> getLocalAgents() {
        return localAgents.list();
    }

    public Set<Gateway> getGateways() {
        return Collections.unmodifiableSet(gates);
    }

    public Receipt send(Message message) throws MsnosException {
        checkCloudAlive();

        proto.info("TX: {} {} {} {}", message.getType(), message.getFrom(), message.getTo(), message.getData());

        Message signed = sign(message);
        MultiGatewayReceipt res = new MultiGatewayReceipt(signed);
        for (Gateway gate : gates) {
            try {
                final Receipt receipt = gate.send(this, signed);
                res.add(receipt);
                if (receipt.getStatus() == Message.Status.DELIVERED)
                    break;
            } catch (IOException ex) {
                log.warn("Unable to send message " + message + " trough gateway " + gate, ex);
            }
        }

        if (res.size() == 0)
            throw new MsnosException("Unable to send message " + message, MsnosException.Code.SEND_FAILED);

        return res;
    }

    void onJoin(LocalAgent agent) throws MsnosException {
        checkCloudAlive();

        log.debug("Local agent joined: {}", agent);
        localAgents.add(agent);

        final Status status = synchronizer.start(agent);
        try {
            send(new MessageBuilder(Message.Type.PRS, agent, this).with(new Presence(true)).make());
            send(new MessageBuilder(Message.Type.DSC, agent, this).make());
            synchronizer.wait(status);
        } finally {
            synchronizer.remove(status);
        }
    }

    void onLeave(LocalAgent agent) throws MsnosException {
        checkCloudAlive();

        send(new MessageBuilder(Message.Type.PRS, agent, this).with(new Presence(false)).make());
        log.debug("Local agent left: {}", agent);
        localAgents.remove(agent.getIden());
    }

    private void checkCloudAlive() throws MsnosException {
        if (gates.size() == 0 || synchronizer == null)
            throw new MsnosException("This cloud is not connected as it is a mirror of a remote one", MsnosException.Code.NOT_CONNECTED);
    }

    public Listener  addListener(com.workshare.msnos.core.Cloud.Listener listener) {
        return caster.addListener(listener);
    }

    public void removeListener(com.workshare.msnos.core.Cloud.Listener listener) {
        log.debug("Removing listener: {}", listener);
        caster.removeListener(listener);
    }

    private void process(Message message) throws IOException {
        if (validators.isValid(message)) {
            proto.info("RX: {} {} {} {}", message.getType(), message.getFrom(), message.getTo(), message.getData());

            boolean processed = message.getData().process(message, internal);
            if (!processed)
                caster.dispatch(message);
            
            postProcess(message);
        } else {
            proto.debug("NN: {} {} {} {}", message.getType(), message.getFrom(), message.getTo(), message.getData());
        }
    }

    private void postProcess(Message message) {
        final Iden from = message.getFrom();

        touch(remoteAgents.get(from));
        touch(remoteClouds.get(from));

        synchronizer.process(message);
    }

    private void touch(RemoteEntity entity) {
        if (entity != null)
            entity.touch();
    }

    public void removeFaultyAgent(RemoteEntity agent) {
        RemoteEntity result = remoteAgents.remove(agent.getIden());
        if (result != null)
            caster.dispatch(new MessageBuilder(Message.Type.FLT, this, this).with(new FltPayload(agent.getIden())).make());
    }

    private Message sign(Message message) {
        if (signid == null)
            return message;
        try {
            return signer.signed(message, signid);
        } catch (IOException e) {
            log.warn("Failed to sign message {} using key {}", message, signid);
            return message;
        }
    }
}

