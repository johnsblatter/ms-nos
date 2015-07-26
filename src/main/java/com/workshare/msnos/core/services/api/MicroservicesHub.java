
package com.workshare.msnos.core.services.api;

import static com.workshare.msnos.core.Message.Type.PRS;
import static com.workshare.msnos.core.Message.Type.QNE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpiringMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.core.cloud.LocalAgent;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.HealthcheckPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.services.api.RestApi.Type;
import com.workshare.msnos.core.services.api.routing.ApiRepository;
import com.workshare.msnos.soup.threading.ExecutorServices;

public class MicroservicesHub {

    private static final ScheduledExecutorService DEFAULT_EXECUTOR = ExecutorServices.newSingleThreadScheduledExecutor();

    private static final Long ENQUIRY_EXPIRE = Long.getLong("com.ws.msnos.microservice.enquiry.timeout", 60);

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final Map<Iden, RemoteMicroservice> remoteServices;
    private final ApiRepository apis;
    private final Cloud cloud;
    private final Map<UUID, Iden> enquiries;
    private final ScheduledExecutorService executor;

    public MicroservicesHub(Cloud cloud) {
        this(cloud, DEFAULT_EXECUTOR);
    }

    public MicroservicesHub(Cloud cloud, ScheduledExecutorService executor) {
        this.cloud = cloud;
        this.cloud.addSynchronousListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                try {
                    doProcess(message);
                } catch (MsnosException e) {
                    log.error("Error processing message {}", e);
                }
            }
        });

        remoteServices = new ConcurrentHashMap<Iden, RemoteMicroservice>();
        apis = new ApiRepository();

        this.executor = executor;
        this.enquiries = ExpiringMap.builder().expiration(ENQUIRY_EXPIRE, TimeUnit.SECONDS).build();

        Healthchecker healthcheck = new Healthchecker(this, executor);
        healthcheck.start();
    }

    protected void process(Message message, Endpoint.Type endpoint) {
        cloud.process(message, endpoint.toString());
    }

    public Cloud getCloud() {
        return cloud;
    }

    public ApiRepository getApis() {
        return apis;
    }

    public boolean canServe(String path) {
        return getApis().canServe(path);
    }

    public RestApi searchApi(Microservice microservice, String path) {
        return getApis().searchApi(microservice, path);
    }

    public RestApi searchApiById(long id) {
        return getApis().searchApiById(id);
    }

    public List<RemoteMicroservice> getMicroServices() {
        return Collections.unmodifiableList(new ArrayList<RemoteMicroservice>(remoteServices.values()));
    }

    void onJoin(LocalMicroservice microservice) throws MsnosException {
        LocalAgent agent = microservice.getAgent();
        agent.join(cloud);

        Message message = new MessageBuilder(Message.Type.ENQ, agent, cloud).make();
        agent.send(message);
    }

    void onLeave(LocalMicroservice microservice) throws MsnosException {
        LocalAgent agent = microservice.getAgent();
        agent.leave();
    }

    private void doProcess(Message message) throws MsnosException {
        log.debug("Handling message {}", message);
        switch (message.getType()) {
            case QNE:
                processQNE(message);
                break;
            case FLT:
                processFault(message);
                break;
            case PRS:
                processPresence(message);
                break;
            case HCK:
                processHealthcheck(message);
                break;
            default:
                break;
        }

        enquiryMicroserviceIfUnknown(message);
        processLocalMicroservices(message);
    }

    private void processLocalMicroservices(Message message) {
        
    }

    private void enquiryMicroserviceIfUnknown(final Message message) {
        final Iden from = message.getFrom();
        if (!remoteServices.containsKey(from)) {
            if (from.getType() == Iden.Type.AGT && message.getType() != PRS)
                if (enquiries.get(from.getUUID()) == null) {
                    enquiries.put(from.getUUID(), from);
                    executor.schedule(new Runnable(){
                        @Override
                        public void run() {
                            if (!remoteServices.containsKey(from)) {
                                log.warn("Enquiring unknown microservice {} on message {} received", from, message.getType());
                                try {
                                    cloud.send(new MessageBuilder(Message.Type.ENQ, cloud, from).make());
                                } catch (MsnosException e) {
                                    log.warn("Unexpected exception while enquiring "+from, e);
                                }
                            } else {
                                log.debug("No need to enquiry microservice {} on message {} received", from, message.getType());
                            }
                        }}, ENQUIRY_EXPIRE/5, TimeUnit.SECONDS);
                }
        }
    }

    private void processHealthcheck(Message message) {
        final HealthcheckPayload payload = ((HealthcheckPayload) message.getData());
        final Iden iden = payload.getIden();
        final RemoteMicroservice remote = remoteServices.get(iden);
        if (remote == null) {
            log.warn("Received health report on service {} not present in the cloud", iden);
            return;
        }

        if (payload.isWorking()) {
            log.debug("Marking remote {} as working after cloud message received", remote.getName());
            remote.markWorking();
        } else {
            log.info("Marking remote {} as faulty after cloud message received", remote.getName());
            remote.markFaulty();
        }
    }

    private void processQNE(Message message) throws MsnosException {
        QnePayload qnePayload = ((QnePayload) message.getData());

        final Iden iden = message.getFrom();
        RemoteAgent remoteAgent = cloud.find(iden);

        if (remoteAgent != null) {
            RemoteMicroservice remote;
            Iden remoteKey = remoteAgent.getIden();

            if (remoteServices.containsKey(remoteKey)) {
                remote = remoteServices.get(remoteKey);
                remote.setApis(qnePayload.getApis());
            } else {
                remote = new RemoteMicroservice(qnePayload.getName(), remoteAgent, new HashSet<RestApi>(qnePayload.getApis()));
                remoteServices.put(remoteKey, remote);
            }

            registerRemoteMsnosEndpoints(remote);

            apis.register(remote);
        }
    }

    private void registerRemoteMsnosEndpoints(RemoteMicroservice remote) throws MsnosException {
        Set<RestApi> remoteApis = remote.getApis();
        for (RestApi restApi : remoteApis) {
            if (restApi.getType() == RestApi.Type.MSNOS_HTTP) {
                final HttpEndpoint endpoint = new HttpEndpoint(remote, restApi);
                cloud.registerRemoteMsnosEndpoint(endpoint);
            }
        }
    }

    private void processFault(Message message) {
        Iden about = ((FltPayload) message.getData()).getAbout();
        removeMicroservice(about);
    }

    private void processPresence(Message message) {
        boolean leaving = (false == ((Presence) message.getData()).isPresent());
        if (leaving)
            removeMicroservice(message.getFrom());
    }

    private void removeMicroservice(Iden about) {
        if (remoteServices.containsKey(about)) {
            apis.unregister(remoteServices.get(about));
            remoteServices.remove(about);
        }
    }

    public RemoteMicroservice getRemoteMicroService(Iden iden) {
        return remoteServices.get(iden);
    }

    void publish(LocalMicroservice microservice, RestApi... apis) throws MsnosException {
        LocalAgent agent = microservice.getAgent();

        Message message = new MessageBuilder(QNE, agent, cloud).with(new QnePayload(microservice.getName(), apis)).make();
        cloud.send(message);

        handleMsnosApis(microservice, apis);
    }

    private void handleMsnosApis(LocalMicroservice microservice, RestApi... apis) throws MsnosException {
        Set<RestApi> msnosApis = new HashSet<RestApi>();
        for (RestApi api : apis) {
            if (api.getType() == Type.MSNOS_HTTP) {
                msnosApis.add(api);
            }
        }

        if (msnosApis.size() == 0)
            return;

        final LocalAgent agent = microservice.getAgent();
        msnosApis = RestApi.ensureHostIsPresent(agent, msnosApis);
        log.debug("Registering msnos apis {} for agent {}", msnosApis, agent.getIden().getUUID());

        for (RestApi api : msnosApis) {
            cloud.registerLocalMsnosEndpoint(new HttpEndpoint(microservice, api));
        }

        cloud.send(new MessageBuilder(PRS, agent, cloud).with(new Presence(true, agent)).make());
    }


    public void synchronize(final long amount, final TimeUnit unit) throws MsnosException {
        
        final Message[] messages = new Message[] {
            new MessageBuilder(Message.Type.DSC, cloud, cloud).make(),
            new MessageBuilder(Message.Type.ENQ, cloud, cloud).make(),
        };
        
        long allotted = amount/messages.length;
        for (Message message : messages) {
            Receipt receipt = cloud.sendSync(message);
            try {
                receipt.waitForDelivery(allotted, unit);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
    }
}