package com.workshare.msnos.usvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.ApiRepository;

public class Microcloud {

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final Map<Iden, RemoteMicroservice> microServices;
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
                    process(message);
                } catch (MsnosException e) {
                    log.error("Error processing message {}", e);
                }
            }
        });

        microServices = new ConcurrentHashMap<Iden, RemoteMicroservice>();
        passiveServices = new ConcurrentHashMap<UUID, PassiveService>();
        apis = new ApiRepository();

        healthcheck = new Healthchecker(this, executor);
        healthcheck.run();
    }

    public Listener addListener(Listener listener) {
        return cloud.addListener(listener);
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
        return Collections.unmodifiableList(new ArrayList<RemoteMicroservice>(microServices.values()));
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

    private void process(Message message) throws MsnosException {
        if (message.getType() == Message.Type.QNE) {
            processQNE(message);
        }
        if (message.getType() == Message.Type.FLT) {
            processFault(message);
        }
    }

    private void processQNE(Message message) {
        QnePayload qnePayload = ((QnePayload) message.getData());

        final Iden iden = message.getFrom();
        RemoteAgent remoteAgent = cloud.find(iden);

        if (remoteAgent != null) {
            RemoteMicroservice remote;
            Iden remoteKey = remoteAgent.getIden();

            if (microServices.containsKey(remoteKey)) {
                remote = microServices.get(remoteKey);
                remote.addApis(qnePayload.getApis());
            } else {
                remote = new RemoteMicroservice(qnePayload.getName(), remoteAgent, new HashSet<RestApi>(qnePayload.getApis()));
                microServices.put(remoteKey, remote);
            }

            apis.register(remote);
        }
    }

    private void processFault(Message message) {
        Iden about = ((FltPayload) message.getData()).getAbout();

        if (microServices.containsKey(about)) {
            apis.unregister(microServices.get(about));
            microServices.remove(about);
        }
    }

    void publish(Microservice microservice, RestApi... apis) throws MsnosException {
        LocalAgent agent = microservice.getAgent();
        Message message = new MessageBuilder(Message.Type.QNE, agent, cloud).with(new QnePayload(microservice.getName(), apis)).make();

        cloud.send(message);
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