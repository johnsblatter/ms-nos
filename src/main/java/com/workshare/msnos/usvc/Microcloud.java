package com.workshare.msnos.usvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Cloud.Listener;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.PassiveAgent;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.HealthcheckPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.RestApi.Type;
import com.workshare.msnos.usvc.api.routing.ApiRepository;

import static com.workshare.msnos.core.Message.Type.*;

public class Microcloud {

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final Map<Iden, RemoteMicroservice> remoteServices;
    private final Map<UUID, PassiveService> passiveServices;
    private final ApiRepository apis;
    private final Healthchecker healthcheck;
    private final Cloud cloud;

    public Microcloud(Cloud cloud) {
        this(cloud, ExecutorServices.newSingleThreadScheduledExecutor());
    }

    public Microcloud(Cloud cloud, ScheduledExecutorService executor) {
        this.cloud = cloud;
        this.cloud.addListener(new Cloud.Listener() {
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
        passiveServices = new ConcurrentHashMap<UUID, PassiveService>();
        apis = new ApiRepository();

        healthcheck = new Healthchecker(this, executor);
        healthcheck.start();
    }

    public Receipt send(Message message) throws MsnosException {
        return cloud.send(message);
    }

    public void process(Message message) {
        cloud.process(message);
    }

    public Listener addListener(Listener listener) {
        return cloud.addListener(listener);
    }

    public void removeListener(Listener listener) {
        cloud.removeListener(listener);
    }

    public Cloud getCloud() {
        return cloud;
    }

    public ApiRepository getApis() {
        return apis;
    }

    public PassiveService searchPassives(UUID search) {
        return passiveServices.get(search);
    }

    public RestApi searchApi(Microservice microservice, String name, String path) {
        return getApis().searchApi(microservice, name, path);
    }

    public RestApi searchApiById(long id) throws Exception {
        return getApis().searchApiById(id);
    }

    public List<RemoteMicroservice> getMicroServices() {
        return Collections.unmodifiableList(new ArrayList<RemoteMicroservice>(remoteServices.values()));
    }

    public List<PassiveService> getPassiveServices() {
        return Collections.unmodifiableList(new ArrayList<PassiveService>(passiveServices.values()));
    }

    void onJoin(Microservice microservice) throws MsnosException {
        LocalAgent agent = microservice.getAgent();
        agent.join(cloud);

        Message message = new MessageBuilder(Message.Type.ENQ, agent, cloud).make();
        agent.send(message);
    }

    public void onLeave(Microservice microservice) throws MsnosException {
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
    }

    private void enquiryMicroserviceIfUnknown(Message message) throws MsnosException {
        final Iden from = message.getFrom();
        if (from.getType() == Iden.Type.AGT && message.getType() != PRS)
            if (!remoteServices.containsKey(from)) {
                log.warn("Enquiring unknown microservice {}", from);
                send(new MessageBuilder(Message.Type.ENQ, cloud, from).make());
            }
   }

    private void processHealthcheck(Message message) {
        final HealthcheckPayload payload = ((HealthcheckPayload) message.getData());
        final Iden iden = payload.getIden();
        final RemoteMicroservice remote = remoteServices.get(iden);
        if (remote == null) {
            if (!cloud.containsAgent(iden) && !passiveServices.containsKey(iden)) {
                log.warn("Received a health status message about service {} not present in the cloud", iden);
            }
            return;
        }

        if (payload.isWorking()) {
            log.debug("Marking remote {} as working after cloud message received", remote);
            remote.markWorking();
        } else {
            log.info("Marking remote {} as faulty after cloud message received", remote);
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
            if (restApi.getType() == RestApi.Type.MSNOS_HTTP)
                cloud.registerRemoteMsnosEndpoint(new HttpEndpoint(remote, restApi));
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

    void publish(Microservice microservice, RestApi... apis) throws MsnosException {
        LocalAgent agent = microservice.getAgent();

        Message message = new MessageBuilder(QNE, agent, cloud).with(new QnePayload(microservice.getName(), apis)).make();
        cloud.send(message);

        handleMsnosApis(microservice, apis);
    }

    private void handleMsnosApis(Microservice microservice, RestApi... apis) throws MsnosException {
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

    void onJoin(PassiveService passive) throws MsnosException {
        passiveServices.put(passive.getUuid(), passive);

        RestApi restApi = new RestApi(passive.getName(), passive.getHealthCheckUri(), passive.getPort(), passive.getHost(), RestApi.Type.HEALTHCHECK, false);

        publish(passive, restApi);
    }

    void publish(PassiveService passiveService, RestApi... apis) throws MsnosException {
        if (!passiveServices.containsKey(passiveService.getUuid()))
            throw new IllegalArgumentException("Cannot publish passive restApis that are from services which are not joined to the Cloud! ");

        PassiveAgent agent = passiveService.getAgent();
        Message message = new MessageBuilder(Message.Type.QNE, agent, cloud).with(new QnePayload(passiveService.getName(), apis)).make();

        cloud.send(message);
    }
}