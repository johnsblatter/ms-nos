package com.workshare.msnos.core;

import com.workshare.msnos.core.cloud.AgentWatchdog;
import com.workshare.msnos.core.cloud.AgentsList;
import com.workshare.msnos.core.cloud.JoinSynchronizer;
import com.workshare.msnos.core.cloud.JoinSynchronizer.Status;
import com.workshare.msnos.core.cloud.Multicaster;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.security.Signer;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.ExecutorServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

public class Cloud implements Identifiable {

    private static Logger log = LoggerFactory.getLogger(Cloud.class);
    private static Logger proto = LoggerFactory.getLogger("protocol");

    public static interface Listener {
        public void onMessage(Message message);
    }

    private final Iden iden;
    private final String signid;
    private final AgentsList<LocalAgent> localAgents;
    private final AgentsList<RemoteAgent> remoteAgents;

    transient private final Set<Gateway> gates;
    transient private final Multicaster caster;
    transient private final JoinSynchronizer synchronizer;
    transient private final Signer signer;
    transient private final Internal internal;

    public class Internal {
        public AgentsList<RemoteAgent> remoteAgents() {
            return remoteAgents;
        }

        public Cloud cloud() {
            return Cloud.this;
        }
    }

    public Cloud(UUID uuid) throws MsnosException {
        this(uuid, null, Gateways.all(), new JoinSynchronizer());
    }

    public Cloud(UUID uuid, String signid) throws MsnosException {
        this(uuid, signid, Gateways.all(), new JoinSynchronizer());
    }

    public Cloud(UUID uuid, String signid, Set<Gateway> gates, JoinSynchronizer synchronizer) {
        this(uuid, signid, new Signer(), gates, synchronizer, new Multicaster(), ExecutorServices.newSingleThreadScheduledExecutor());
    }

    public Cloud(UUID uuid, String signid, Signer signer, Set<Gateway> gates, JoinSynchronizer synchronizer, Multicaster multicaster, ScheduledExecutorService executor) {
        this.iden = new Iden(Iden.Type.CLD, uuid);
        this.localAgents = new AgentsList<LocalAgent>();
        this.remoteAgents = new AgentsList<RemoteAgent>();
        this.caster = multicaster;
        this.gates = gates;
        this.signer = signer;
        this.signid = signid;
        this.synchronizer = synchronizer;

        for (Gateway gate : gates) {
            gate.addListener(this, new Gateway.Listener() {
                @Override
                public void onMessage(Message message) {
                    process(message);
                }
            });
        }

        this.internal = new Internal();
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
                res.add(gate.send(this, signed));
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
            send(new MessageBuilder(Message.Type.PRS, agent, this).sequence(agent.getSeq()).with(new Presence(true)).make());
            send(new MessageBuilder(Message.Type.DSC, agent, this).sequence(agent.getSeq()).make());
            synchronizer.wait(status);
        } finally {
            synchronizer.remove(status);
        }
    }

    void onLeave(LocalAgent agent) throws MsnosException {
        checkCloudAlive();

        send(new MessageBuilder(Message.Type.PRS, agent, this).sequence(agent.getSeq()).with(new Presence(false)).make());
        log.debug("Local agent left: {}", agent);
        localAgents.remove(agent.getIden());
    }

    private void checkCloudAlive() throws MsnosException {
        if (gates.size() == 0 || synchronizer == null)
            throw new MsnosException("This cloud is not connected as it is a mirror of a remote one", MsnosException.Code.NOT_CONNECTED);
    }

    public Listener addListener(com.workshare.msnos.core.Cloud.Listener listener) {
        return caster.addListener(listener);
    }

    public void removeListener(com.workshare.msnos.core.Cloud.Listener listener) {
        log.debug("Removing listener: {}", listener);
        caster.removeListener(listener);
    }

    private void process(Message message) {

        if (isProcessable(message)) {
            proto.info("RX: {} {} {} {}", message.getType(), message.getFrom(), message.getTo(), message.getData());

            boolean processed = message.getData().process(message, internal);
            if (!processed)
                dispatch(message);
        } else {
            proto.debug("NN: {} {} {} {}", message.getType(), message.getFrom(), message.getTo(), message.getData());
        }

        remoteAgents.touch(message.getFrom());
        for (RemoteAgent agent : remoteAgents.list()) agent.setSeq(message.getSeq());
        synchronizer.process(message);
    }

    private boolean isProcessable(Message message) {
        if (isComingFromALocalAgent(message)) {
            log.debug("Skipped message sent from a local agent: {}", message);
            return false;
        }

        if (isAddressedToARemoteAgent(message)) {
            log.debug("Skipped message addressed to a remote agent: {}", message);
            return false;
        }

        if (isOutOfSequence(message)) {
            log.debug("Skipped message sent out of order: {} ", message);
            return false;
        }

        if (!isAddressedToTheLocalCloud(message)) {
            log.debug("Skipped message addressed to another cloud: {}", message);
            return false;
        }

        if (!isCorrectlySigned(message)) {
            log.debug("Skipped message incorrectly signed: {} - key: ", message, signid);
            return false;
        }

        return true;
    }

    private boolean isOutOfSequence(Message message) {
        for (RemoteAgent remote : getRemoteAgents()) {
            if (message.getSeq() <= remote.getSeq()) {
                return true;
            }
        }
        return false;
    }

    private boolean isCorrectlySigned(Message message) {
        Message signed = sign(message);
        return parseNull(message.getSig()).equals(parseNull(signed.getSig()));
    }

    private boolean isAddressedToARemoteAgent(Message message) {
        return remoteAgents.containsKey(message.getTo());
    }

    private boolean isComingFromALocalAgent(Message message) {
        return localAgents.containsKey(message.getFrom());
    }

    private boolean isAddressedToTheLocalCloud(Message message) {
        final Iden to = message.getTo();
        return to.equals(iden) || localAgents.containsKey(to);
    }

    public void removeFaultyAgent(RemoteAgent agent) {
        RemoteAgent result = remoteAgents.remove(agent.getIden());
        if (result != null)
            dispatch(new MessageBuilder(Message.Type.FLT, this, this).with(iden.getUUID()).with(new FltPayload(agent.getIden())).make());
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

    private void dispatch(Message message) {
        caster.dispatch(message);
    }

    private String parseNull(String signature) {
        return (signature == null ? "" : signature);
    }

}
