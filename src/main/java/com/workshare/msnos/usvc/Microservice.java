package com.workshare.msnos.usvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;

public class Microservice {

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final String name;
    private final LocalAgent agent;
    private final Healthchecker healthcheck;
    private final List<RestApi> localApis;
    private final List<RemoteMicroservice> microServices;
    private final Map<String, ApiList> remoteApis;

    private Cloud cloud;

    public Microservice(String name) {
        this(name, Executors.newSingleThreadScheduledExecutor());
    }

    public Microservice(String name, ScheduledExecutorService executor) {
        agent = new LocalAgent(UUID.randomUUID());
        cloud = null;
        this.name = name;

        localApis = new ArrayList<RestApi>();
        microServices = new ArrayList<RemoteMicroservice>();
        remoteApis = new ConcurrentHashMap<String, ApiList>();
        healthcheck = new Healthchecker(this, executor);
    }

    public LocalAgent getAgent() {
        return agent;
    }

    public List<RemoteMicroservice> getMicroServices() {
        return Collections.unmodifiableList(microServices);
    }

    public List<RestApi> getAllRemoteRestApis() {
        List<RestApi> result = new ArrayList<RestApi>();
        for (ApiList api : remoteApis.values()) {
            result.addAll(api.getAll());
        }
        return Collections.unmodifiableList(result);
    }

    public void join(Cloud nimbus) throws MsnosException {
        agent.join(nimbus);
        this.cloud = nimbus;
        cloud.addListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                try {
                    process(message);
                } catch (MsnosException e) {
                    log.error("Error processing message {}", e);
                }
            }
        });
        Message message = new MessageBuilder(Message.Type.ENQ,agent,cloud).make();
        agent.send(message);
        healthcheck.run();
    }

    public void publish(RestApi... api) throws MsnosException {
        Message message = new MessageBuilder(Message.Type.QNE,agent,cloud).with(new QnePayload(name, api)).make();
        agent.send(message);
        localApis.addAll(Arrays.asList(api));
    }

    private void process(Message message) throws MsnosException {
        if (!message.getFrom().equals(agent.getIden())) {
            if (message.getType() == Message.Type.ENQ) {
                processENQ();
            }
            if (message.getType() == Message.Type.QNE) {
                processQNE(message);
            }
            if (message.getType() == Message.Type.FLT) {
                processFault(message);
            }
        }
    }

    private void processENQ() throws MsnosException {
        Message message = new MessageBuilder(Message.Type.QNE,agent,cloud).with(new QnePayload(name, new HashSet<RestApi>(localApis))).make();
        agent.send(message);
    }

    private void processQNE(Message message) {
        synchronized (microServices) {
            QnePayload qnePayload = ((QnePayload) message.getData());
            String name = qnePayload.getName();
            Set<RestApi> apis = new HashSet<RestApi>(qnePayload.getApis());
            RemoteAgent remoteAgent = null;
            for (RemoteAgent agent : cloud.getRemoteAgents()) {
                if (agent.getIden().equals(message.getFrom())) {
                    remoteAgent = agent;
                    break;
                }
            }
            RemoteMicroservice remote = new RemoteMicroservice(name, remoteAgent, apis);
            microServices.add(remote);
            processRestApis(remote, remote.getApis());
        }
    }

    private void processRestApis(RemoteMicroservice remote, Set<RestApi> apis) {
        for (RestApi rest : apis) {
            String key = rest.getName() + rest.getPath();
            if (remoteApis.containsKey(key)) {
                remoteApis.get(key).add(remote, rest);
            } else {
                ApiList apiList = new ApiList();
                apiList.add(remote, rest);
                remoteApis.put(key, apiList);
            }
        }
    }

    private void processFault(Message message) {
        FltPayload fault = (FltPayload) message.getData();
        synchronized (microServices) {
            for (int i = 0; i < microServices.size(); i++) {
                if (microServices.get(i).getAgent().getIden().equals(fault.getAbout())) {
                    RemoteMicroservice faulty = microServices.get(i);
                    microServices.remove(faulty);
                    removeRestApis(faulty, faulty.getApis());
                    break;
                }
            }
        }
    }

    private void removeRestApis(RemoteMicroservice faulty, Set<RestApi> apis) {
        for (RestApi rest : apis) {
            String key = rest.getName() + rest.getPath();
            if (remoteApis.containsKey(key)) {
                ApiList apiList = remoteApis.get(key);
                apiList.remove(faulty);
                break;
            }
        }
    }

    public RestApi searchApiById(long id) throws Exception {
        Collection<ApiList> apiListCol = remoteApis.values();
        for (ApiList apiList : apiListCol) {
            for (RestApi rest : apiList.getAll()) {
                if (rest.getId() == id) {
                    return rest;
                }
            }
        }
        return null;
    }

    public RestApi searchApi(String name, String path) throws Exception {
        String key = name + path;
        ApiList apiList = remoteApis.get(key);
        return apiList == null ? null : apiList.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Microservice that = (Microservice) o;
        return this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + localApis.hashCode();
        result = 31 * result + agent.hashCode();
        result = 31 * result + cloud.hashCode();
        return result;
    }
}
