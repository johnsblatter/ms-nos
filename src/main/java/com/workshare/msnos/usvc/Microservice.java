package com.workshare.msnos.usvc;

import com.workshare.msnos.core.*;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.usvc.api.RestApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

public class Microservice {

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final String name;
    private final LocalAgent agent;
    private final Healthchecker healthcheck;
    private final Location location;

    private MicroCloud microCloud;

    public Microservice(String name) {
        this(name, new LocalAgent(UUID.randomUUID()), ExecutorServices.newSingleThreadScheduledExecutor());
    }

    public Microservice(String name, LocalAgent agent) {
        this(name, agent, ExecutorServices.newSingleThreadScheduledExecutor());
    }

    public Microservice(String name, LocalAgent agent, ScheduledExecutorService executor) {
        this.name = name;
        this.agent = agent;
        this.healthcheck = new Healthchecker(this, executor);
        this.location = Location.computeMostPreciseLocation(agent.getEndpoints());
    }

    public String getName() {
        return name;
    }

    public LocalAgent getAgent() {
        return agent;
    }

    public Location getLocation() {
        return location;
    }

    public List<PassiveService> getPassiveServices() {
        return Collections.unmodifiableList(new ArrayList<PassiveService>(microCloud.getPassiveServices().values()));
    }

    public List<RemoteMicroservice> getMicroServices() {
        return Collections.unmodifiableList(new ArrayList<RemoteMicroservice>(microCloud.getMicroservices().values()));
    }

    public RestApi searchApiById(long id) throws Exception {
        return microCloud.getApis().searchApiById(id);
    }

    public RestApi searchApi(String name, String path) {
        return microCloud.getApis().searchApi(this, name, path);
    }

    public void passiveJoin(PassiveService passiveService) {
        microCloud.passiveJoin(passiveService);
    }

    public void passivePublish(PassiveService passiveService, RestApi... apis) throws MsnosException, IllegalArgumentException {
        microCloud.passivePublish(passiveService, this, apis);
    }

    public void join(Cloud nimbus) throws MsnosException {
        if (this.microCloud != null)
            throw new IllegalArgumentException("The same instance of a microservice cannot join different clouds!");

        microCloud = new MicroCloud(nimbus);
        microCloud.onJoin(this);

        microCloud.addListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                try {
                    process(message);
                } catch (MsnosException e) {
                    log.error("Error processing message {}", e);
                }
            }
        });

        healthcheck.run();
    }

    public void publish(RestApi... api) throws MsnosException {
        microCloud.publish(this, api);
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
        Message message = new MessageBuilder(Message.Type.QNE, agent, microCloud.getCloud()).with(new QnePayload(name, new HashSet<RestApi>(microCloud.getLocalApis()))).make();
        agent.send(message);
    }

    private void processQNE(Message message) {
        QnePayload qnePayload = ((QnePayload) message.getData());

        RemoteAgent remoteAgent = null;
        for (RemoteAgent agent : microCloud.getCloud().getRemoteAgents()) {
            if (agent.getIden().equals(message.getFrom())) {
                remoteAgent = agent;
                break;
            }
        }

        if (remoteAgent != null) {
            RemoteMicroservice remote;
            Iden remoteKey = remoteAgent.getIden();

            Map<Iden, RemoteMicroservice> microServices = microCloud.getMicroservices();
            if (microServices.containsKey(remoteKey)) {
                remote = microServices.get(remoteKey);
                remote.addApis(qnePayload.getApis());
            } else {
                remote = new RemoteMicroservice(qnePayload.getName(), remoteAgent, new HashSet<RestApi>(qnePayload.getApis()));
                microServices.put(remoteKey, remote);
            }

            microCloud.getApis().register(remote);
        }
    }

    private void processFault(Message message) {
        Iden about = ((FltPayload) message.getData()).getAbout();
        Map<Iden, RemoteMicroservice> microServices = microCloud.getMicroservices();

        if (microServices.containsKey(about)) {
            microCloud.getApis().unregister(microServices.get(about));
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
        result = 31 * result + agent.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + microCloud.getCloud().hashCode();
        return result;
    }
}
