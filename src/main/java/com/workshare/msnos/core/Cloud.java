package com.workshare.msnos.core;

import static com.workshare.msnos.core.Message.Type.PRS;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.jodah.expiringmap.ExpiringMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.cloud.AgentWatchdog;
import com.workshare.msnos.core.cloud.IdentifiablesList;
import com.workshare.msnos.core.cloud.JoinSynchronizer;
import com.workshare.msnos.core.cloud.JoinSynchronizer.Status;
import com.workshare.msnos.core.cloud.Multicaster;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.security.Signer;
import com.workshare.msnos.soup.ShutdownHooks;
import com.workshare.msnos.soup.ShutdownHooks.Hook;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.soup.time.SystemTime;

public class Cloud implements Identifiable {

    private static final Long ENQUIRY_EXPIRE = Long.getLong("com.ws.msnos.agent.enquiry.timeout", 30);

    private static final ScheduledExecutorService DEFAULT_SCHEDULER = ExecutorServices.newSingleThreadScheduledExecutor();

    private static final Logger log = LoggerFactory.getLogger(Cloud.class);

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

    transient private final Set<Gateway> gates;
    transient private final JoinSynchronizer synchronizer;
    transient private final Ring ring;
    transient private final Signer signer;
    transient private final Internal internal;
    transient private final Sender sender;
    transient private final Receiver receiver;
    transient private final Map<UUID, Iden> enquiries;

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

    public Cloud(UUID uuid, String signid, Set<Gateway> gates, JoinSynchronizer synchronizer, Long instanceId) {
        this(uuid, signid, new Signer(), new Sender(), gates, synchronizer, new Multicaster(), DEFAULT_SCHEDULER, instanceId);
    }

    Cloud(final UUID uuid, String signid, Signer signer, Sender sender, Set<Gateway> gates, JoinSynchronizer synchronizer, Multicaster multicaster, ScheduledExecutorService executor, Long instanceId) {
        this.iden = new Iden(Iden.Type.CLD, uuid, instanceId);

        this.enquiries = ExpiringMap.builder().expiration(ENQUIRY_EXPIRE, TimeUnit.SECONDS).build();

        this.localAgents = new IdentifiablesList<LocalAgent>();
        this.remoteAgents = new IdentifiablesList<RemoteAgent>();
        this.remoteClouds = new IdentifiablesList<RemoteEntity>();

        this.seq = new AtomicLong(SystemTime.asMillis());
        this.gates = Collections.unmodifiableSet(gates);
        this.internal = new Internal();

        this.signer = signer;
        this.signid = signid;
        this.synchronizer = synchronizer;
        this.ring = calculateRing(gates);

        this.sender = sender;
        this.receiver = new Receiver(this, gates, multicaster);

        new AgentWatchdog(this, executor).start();

        ShutdownHooks.addHook(new Hook() {
            @Override
            public void run() {
                log.info("Asking all agents to leave...");
                try {
                    for (LocalAgent agent : localAgents.list()) {
                        ensureLeft(agent);
                    }
                } finally {
                    log.info("done!");
                }
            }

            private void ensureLeft(LocalAgent agent) {
                if (agent.getCloud() != null)
                    try {
                        log.debug("- agent {} is leaving...", agent.getIden().getUUID());
                        agent.leave();
                    } catch (Throwable ex) {
                        log.warn("Unexpected exception while enforcing agent to leave the cloud", ex);
                    }
            }

            @Override
            public String name() {
                return "Ensure all agents left the cloud " + uuid;
            }

            @Override
            public int priority() {
                return 0;
            }
        });
    }

    private Ring calculateRing(Set<Gateway> gateways) {
        HashSet<Endpoint> endpoints = new HashSet<Endpoint>();
        for (Gateway gate : gateways) {
            final Set<? extends Endpoint> points = gate.endpoints().all();
            if (points != null)
                endpoints.addAll(points);
        }

        return Ring.make(endpoints);
    }

    @Override
    public String toString() {
        try {
            return Json.toJsonString(this);
        } catch (Throwable any) {
            return super.toString();
        }
    }

    @Override
    public Iden getIden() {
        return iden;
    }

    public boolean containsAgent(Iden iden) {
        return remoteAgents.containsKey(iden) || containsLocalAgent(iden);
    }

    public boolean containsLocalAgent(Iden iden) {
        return localAgents.containsKey(iden);
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
        return gates;
    }

    public Receipt send(Message message) throws MsnosException {
        checkCloudAlive();
        return sender.send(this, sign(message));
    }

    public Receipt sendSync(Message message) throws MsnosException {
        checkCloudAlive();

        final MultiReceipt receipt = new MultiReceipt(message);
        sender.sendSync(this, sign(message), receipt);
        return receipt;
    }

    void onJoin(LocalAgent agent) throws MsnosException {
        checkCloudAlive();

        log.debug("Local agent joined: {}", agent);
        localAgents.add(agent);

        final Status status = synchronizer.start(agent);
        try {
            send(new MessageBuilder(Message.Type.PRS, agent, this).with(new Presence(true, agent)).make());
            send(new MessageBuilder(Message.Type.DSC, agent, this).make());
            synchronizer.wait(status);
        } finally {
            synchronizer.remove(status);
        }
    }

    void onLeave(LocalAgent agent) throws MsnosException {
        checkCloudAlive();

        sendSync(new MessageBuilder(Message.Type.PRS, agent, this).with(new Presence(false, agent)).make());
        log.debug("Local agent left: {}", agent);
        localAgents.remove(agent.getIden());
    }

    private void checkCloudAlive() throws MsnosException {
        if (gates.size() == 0 || synchronizer == null)
            throw new MsnosException("This cloud is not connected as it is a mirror of a remote one", MsnosException.Code.NOT_CONNECTED);
    }

    public Listener addListener(com.workshare.msnos.core.Cloud.Listener listener) {
        return receiver.caster().addListener(listener);
    }

    public void removeListener(com.workshare.msnos.core.Cloud.Listener listener) {
        receiver.caster().removeListener(listener);
    }

    private void enquiryAgentIfNecessary(Message message) {
        final Iden from = message.getFrom();
        if (from.getType() == Iden.Type.AGT && message.getType() != PRS) {
            if (!remoteAgents.containsKey(from))
                if (enquiries.get(from.getUUID()) == null) {
                    enquiries.put(from.getUUID(), from);
                    try {
                        log.warn("Enquiring unknown agent {}", from);
                        final Cloud cloud = internal.cloud();
                        cloud.send(new MessageBuilder(Message.Type.DSC, cloud, from).make());
                    } catch (IOException e) {
                        log.error("Unexpected exception sending message " + message, e);
                    }
                }
        }
    }

    void postProcess(Message message) {
        enquiryAgentIfNecessary(message);

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
        log.warn("Removing faulty agent " + agent);
        RemoteEntity result = remoteAgents.remove(agent.getIden());
        if (result != null)
            receiver.caster().dispatch(new MessageBuilder(Message.Type.FLT, this, this).with(new FltPayload(agent.getIden())).make());
    }

    public void registerRemoteMsnosEndpoint(HttpEndpoint endpoint) throws MsnosException {
        log.debug("Registering remote HTTP endpoint: {}", endpoint);
        for (Gateway gate : gates) {
            if (gate instanceof HttpGateway) {
                gate.endpoints().install(endpoint);
                updateRemoteAgent(endpoint);
                endpoint = null;
            }
        }

        if (endpoint != null) {
            log.warn("Warning: unable to install endpoint, HTTP gateway not found!");
        }
    }

    public void registerLocalMsnosEndpoint(HttpEndpoint endpoint) {
        log.debug("Registering local HTTP endpoint: {}", endpoint);
        LocalAgent agent = localAgents.get(endpoint.getTarget());
        if (agent == null) {
            log.warn("Weird... a local agent registered an httpendpoint, but I do not know him");
            return;
        }

        agent.registerEndpoint(endpoint);
        log.debug("Agent {} updated, added endpoints {}", agent.getIden().getUUID(), endpoint);
    }

    private void updateRemoteAgent(HttpEndpoint endpoint) {
        RemoteAgent agent = remoteAgents.get(endpoint.getTarget());
        if (agent == null) {
            log.warn("Weird... a remote agent registered an httpendpoint, but I do not know him");
            return;
        }

        remoteAgents.add(agent.with(newEndpoints(endpoint, agent)));
        log.debug("Agent {} updated, new endpoints are {}", agent, newEndpoints(endpoint, agent));
    }

    private Set<Endpoint> newEndpoints(HttpEndpoint endpoint, Agent agent) {
        Set<Endpoint> endpoints = new HashSet<Endpoint>();
        endpoints.addAll(agent.getEndpoints());
        endpoints.add(endpoint);
        return endpoints;
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

    public RemoteAgent find(final Iden iden) {
        RemoteAgent remoteAgent = null;
        for (RemoteAgent agent : getRemoteAgents()) {
            if (agent.getIden().equals(iden)) {
                remoteAgent = agent;
                break;
            }
        }
        return remoteAgent;
    }

    public void process(Message message, String gateName) {
        receiver.process(message, gateName);
    }

    public Ring getRing() {
        return ring;
    }

    Internal internal() {
        return internal;
    }
}