package com.workshare.msnos.usvc;

import com.workshare.msnos.core.*;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.ApiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class Microservice {

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final String name;
    private final LocalAgent agent;
    private final Healthchecker healthcheck;
    private final List<RestApi> localApis;
    private final Map<Iden, RemoteMicroservice> microServices;
    private final ApiRepository apis;
    private final Location location;

    private Cloud cloud;

    public Microservice(String name) {
        this(name, new LocalAgent(UUID.randomUUID()), ExecutorServices.newSingleThreadScheduledExecutor());
    }

    public Microservice(String name, LocalAgent agent) {
        this(name, agent, ExecutorServices.newSingleThreadScheduledExecutor());
    }

    public Microservice(String name, LocalAgent agent, ScheduledExecutorService executor) {
        this.name = name;
        this.agent = agent;

        this.localApis = new CopyOnWriteArrayList<RestApi>();
        this.microServices = new ConcurrentHashMap<Iden, RemoteMicroservice>();
        this.healthcheck = new Healthchecker(this, executor);
        this.apis = new ApiRepository();
        this.location = Location.computeMostPreciseLocation(agent.getEndpoints());

        this.cloud = null;
    }

    public LocalAgent getAgent() {
        return agent;
    }

    public Location getLocation() {
        return location;
    }

    public List<RemoteMicroservice> getMicroServices() {
        return Collections.unmodifiableList(new ArrayList<RemoteMicroservice>(microServices.values()));
    }

    public RestApi searchApiById(long id) throws Exception {
        return apis.searchApiById(id);
    }

    public RestApi searchApi(String name, String path) throws Exception {
        return apis.searchApi(this, name, path);
    }

    public void join(Cloud nimbus) throws MsnosException {
        if (this.cloud != null)
            throw new IllegalArgumentException("The same instance of a microservice cannot join different clouds!");

        this.cloud = nimbus;
        agent.join(cloud);
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

        Message message = new MessageBuilder(Message.Type.ENQ, agent, cloud).make();
        agent.send(message);

        healthcheck.run();
    }

    public void publish(RestApi... api) throws MsnosException {
        Message message = new MessageBuilder(Message.Type.QNE, agent, cloud).with(new QnePayload(name, api)).make();
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
        Message message = new MessageBuilder(Message.Type.QNE, agent, cloud).with(new QnePayload(name, new HashSet<RestApi>(localApis))).make();
        agent.send(message);
    }

    // TODO what happens if the remote agent is not recognized?
    private void processQNE(Message message) {
        QnePayload qnePayload = ((QnePayload) message.getData());

        RemoteAgent remoteAgent = null;
        for (RemoteAgent agent : cloud.getRemoteAgents()) {
            if (agent.getIden().equals(message.getFrom())) {
                remoteAgent = agent;
                break;
            }
        }

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

    @Override
    public String toString() {
        return Json.toJsonString(agent);
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
