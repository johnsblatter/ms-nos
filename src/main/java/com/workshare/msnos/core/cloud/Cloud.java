package com.workshare.msnos.core.cloud;

import static com.workshare.msnos.core.Message.Type.PRS;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpiringMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Gateways;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Identifiable;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.MsnosException.Code;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.RemoteEntity;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.cloud.IdentifiablesList.Callback;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.receipts.SingleReceipt;
import com.workshare.msnos.core.routing.Router;
import com.workshare.msnos.core.security.Signer;
import com.workshare.msnos.soup.ShutdownHooks;
import com.workshare.msnos.soup.ShutdownHooks.Hook;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.ExecutorServices;

public class Cloud implements Identifiable {

    private static final Long ENQUIRY_EXPIRE = Long.getLong("com.ws.msnos.agent.enquiry.timeout", 30);

    private static final ScheduledExecutorService DEFAULT_SCHEDULER = ExecutorServices.newSingleThreadScheduledExecutor();

    private static final Logger log = LoggerFactory.getLogger(Cloud.class);

    public static interface Listener {
        public void onMessage(Message message);
    }

    private final Iden iden;
    private final String signid;
    private final IdentifiablesList<LocalAgent> localAgents;
    private final IdentifiablesList<RemoteAgent> remoteAgents;
    private final IdentifiablesList<RemoteEntity> remoteClouds;

    transient private final Set<Gateway> gates;
    transient private final Ring ring;
    transient private final Signer signer;
    transient private final Internal internal;
    transient private final Sender sender;
    transient private final Receiver receiver;
    transient private final Map<UUID, Iden> enquiries;
    transient private final MessageValidators validators;

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
        this(uuid, signid, Gateways.all());
    }

    public Cloud(UUID uuid, String signid, Set<Gateway> gates) {
        this(uuid, signid, new Signer(), null, null, gates, new Multicaster(), DEFAULT_SCHEDULER);
    }

    Cloud(final UUID uuid, String signid, Signer signer, Sender sender, Receiver receiver, Set<Gateway> gates, Multicaster multicaster, ScheduledExecutorService executor) {
        this.iden = new Iden(Iden.Type.CLD, uuid);

        this.enquiries = ExpiringMap.builder().expiration(ENQUIRY_EXPIRE, TimeUnit.SECONDS).build();

        this.localAgents = new IdentifiablesList<LocalAgent>();
        this.remoteAgents = new IdentifiablesList<RemoteAgent>(onRemoteAgentsChange());
        this.remoteClouds = new IdentifiablesList<RemoteEntity>();

        this.gates = Collections.unmodifiableSet(gates);
        this.internal = new Internal();

        this.signer = signer;
        this.signid = signid;
        this.ring = calculateRing(gates);
        this.validators = new MessageValidators(this.internal);
        
        final Router router = new Router(this, gates);
        this.sender = (sender != null) ? sender : new Sender(router);
        this.receiver = (receiver != null) ? receiver : new Receiver(this, gates, multicaster, router);

        addShutdownHook(uuid);

        startAgentWatchdog(executor);
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

    public MessageValidators validators() {
        return validators;
    }
    
    public boolean containsAgent(Iden iden) {
        return remoteAgents.containsKey(iden) || containsLocalAgent(iden);
    }

    public boolean containsLocalAgent(Iden iden) {
        return localAgents.containsKey(iden);
    }

    public RemoteAgent getRemoteAgent(final Iden iden) {
        return remoteAgents.get(iden);
    }

    public LocalAgent getLocalAgent(final Iden iden) {
        return localAgents.get(iden);
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

        final SingleReceipt receipt = SingleReceipt.unknown(message);
        sender.sendSync(this, sign(message), receipt);
        return receipt;
    }

    void onJoin(LocalAgent agent) throws MsnosException {
        checkCloudAlive();

        log.debug("Local agent joined: {}", agent);
        localAgents.add(agent);

        Receipt receipt = sendSync(new MessageBuilder(Message.Type.PRS, agent, this).with(new Presence(true, agent)).make());
        waitForDelivery(receipt, 1, TimeUnit.SECONDS);
        
        sendSync(new MessageBuilder(Message.Type.DSC, agent, this).make());
    }

    private void waitForDelivery(Receipt receipt, final int amount, final TimeUnit unit) throws MsnosException {
        try {
            receipt.waitForDelivery(amount, unit);
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new MsnosException(e.getMessage(), Code.SEND_FAILED);
        }
   }

    void onLeave(LocalAgent agent) throws MsnosException {
        checkCloudAlive();

        sendSync(new MessageBuilder(Message.Type.PRS, agent, this).with(new Presence(false, agent)).make());
        log.debug("Local agent left: {}", agent);
        localAgents.remove(agent.getIden());
    }

    private void checkCloudAlive() throws MsnosException {
        if (gates.size() == 0)
            throw new MsnosException("This cloud is not connected as it is a mirror of a remote one", MsnosException.Code.NOT_CONNECTED);
    }

    public Listener addListener(com.workshare.msnos.core.cloud.Cloud.Listener listener) {
        return receiver.caster().addListener(listener);
    }

    public void removeListener(com.workshare.msnos.core.cloud.Cloud.Listener listener) {
        receiver.caster().removeListener(listener);
    }

    public Listener addSynchronousListener(com.workshare.msnos.core.cloud.Cloud.Listener listener) {
        return receiver.caster().addSynchronousListener(listener);
    }

    private void enquiryAgentIfNecessary(Message message) {
        final Iden from = message.getFrom();
        if (from.getType() == Iden.Type.AGT && message.getType() != PRS) {
            if (!remoteAgents.containsKey(from))
                if (enquiries.get(from.getUUID()) == null) {
                    enquiries.put(from.getUUID(), from);
                    try {
                        log.info("Enquiring unknown agent {}", from);
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

    public void unregisterRemoteMsnosEndpoint(HttpEndpoint endpoint) throws MsnosException {
        log.debug("Unregistering remote HTTP endpoint: {}", endpoint);
        unregisterFromHttpGateway(endpoint);
    }

    private void unregisterFromHttpGateway(HttpEndpoint endpoint) throws MsnosException {
        for (Gateway gate : gates) {
            if (gate instanceof HttpGateway) {
                gate.endpoints().remove(endpoint);
            }
        }
    }

    public void registerRemoteMsnosEndpoint(HttpEndpoint endpoint) throws MsnosException {
        log.debug("Registering remote HTTP endpoint: {}", endpoint);
        registerOnHttpGateway(endpoint);
        registerOnRemoteAgent(endpoint);
    }

    private void registerOnHttpGateway(HttpEndpoint endpoint) throws MsnosException {
        for (Gateway gate : gates) {
            if (gate instanceof HttpGateway) {
                gate.endpoints().install(endpoint);
            }
        }
    }

    private void registerOnRemoteAgent(HttpEndpoint endpoint) {
        RemoteAgent agent = getRemoteAgent(endpoint.getTarget());
        if (agent == null) {
            log.warn("Weird... a remote agent registered an httpendpoint, but I do not know him");
            return;
        }

        if (agent.getEndpoints(Endpoint.Type.HTTP).contains(endpoint))
            return;
            
        agent.update(newEndpoints(endpoint, agent));
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
    
    private void startAgentWatchdog(ScheduledExecutorService executor) {
        new AgentWatchdog(this, executor).start();
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

    private void addShutdownHook(final UUID uuid) {
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

    private Callback<RemoteAgent> onRemoteAgentsChange() {
        return new Callback<RemoteAgent>() {
            @Override
            public void onAdd(RemoteAgent agent) {
                for(Endpoint endpoint : agent.getEndpoints(Endpoint.Type.HTTP)) {
                    try {
                        registerRemoteMsnosEndpoint((HttpEndpoint) endpoint);
                    } catch (Exception e) {
                        log.error("wtf - unable to register http endpoint", e);
                    }
                }
            }

            @Override
            public void onRemove(RemoteAgent agent) {
                for(Endpoint endpoint : agent.getEndpoints(Endpoint.Type.HTTP)) {
                    try {
                        unregisterRemoteMsnosEndpoint((HttpEndpoint) endpoint);
                    } catch (Exception e) {
                        log.error("wtf - unable to register http endpoint", e);
                    }
                }
            }};
    }


}