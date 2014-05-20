package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class Microservice {

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final String name;
    private final List<RestApi> apis;
    private final Agent agent;
    private final List<RemoteMicroservice> microServices;
    private final List<RemoteMicroservice> faultyMicroservices;
    private Cloud cloud;

    public Microservice(String name) {
        this.name = name;
        this.apis = new ArrayList<RestApi>();
        this.agent = new Agent(UUID.randomUUID());
        this.cloud = null;

        microServices = new ArrayList<RemoteMicroservice>();
        faultyMicroservices = new ArrayList<RemoteMicroservice>();
    }

    public String getName() {
        return name;
    }

    public Set<RestApi> getApis() {
        return new HashSet<RestApi>(apis);
    }

    public Agent getAgent() {
        return agent;
    }

    public List<RemoteMicroservice> getMicroServices() {
        return Collections.unmodifiableList(microServices);
    }

    public void join(Cloud nimbus) throws IOException {
        agent.join(nimbus);
        this.cloud = nimbus;
        cloud.addListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                try {
                    process(message);
                } catch (IOException e) {
                    log.error("Error processing message {}", e);
                }
            }
        });
        Message message = new Message(Message.Type.ENQ, agent.getIden(), cloud.getIden(), 2, false, null);
        agent.send(message);
    }

    public void publish(RestApi... api) throws IOException {
        Message message = new Message(Message.Type.QNE, agent.getIden(), cloud.getIden(), 2, false, new QnePayload(name, api));
        agent.send(message);
        apis.addAll(Arrays.asList(api));
    }

    private void process(Message message) throws IOException {
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

    private void processENQ() throws IOException {
        agent.send(new Message(Message.Type.QNE, agent.getIden(), cloud.getIden(), 2, false, new QnePayload(name, getApis())));
    }

    private void processQNE(Message message) {
        synchronized (microServices) {
            QnePayload qnePayload = ((QnePayload) message.getData());
            String name = qnePayload.getName();
            Set<RestApi> remoteApis = new HashSet<RestApi>(qnePayload.getApis());
            RemoteAgent remoteAgent = null;
            for (RemoteAgent agent : cloud.getRemoteAgents()) {
                if (agent.getIden().equals(message.getFrom())) {
                    remoteAgent = agent;
                }
            }
            RemoteMicroservice remote = new RemoteMicroservice(name, remoteAgent, remoteApis);
            microServices.add(remote);
        }
    }

    private void processFault(Message message) {
        FltPayload fault = (FltPayload) message.getData();
        synchronized (microServices) {
            for (int i = 0; i < microServices.size(); i++) {
                if (microServices.get(i).getAgent().getIden().equals(fault.getAbout())) {
                    microServices.remove(microServices.get(i));
                    break;
                }
            }
        }
    }

    public RestApi searchApi(String name, String endpoint) throws Exception {
        synchronized (microServices) {
            for (RemoteMicroservice remote : microServices) {
                for (RestApi rest : remote.getApis()) {
                    if (rest.isFaulty()) faultyMicroservices.add(remote);
                    if (remote.getName().contains(name) && rest.getPath().contains(endpoint) && !faultyMicroservices.contains(remote))
                        return rest;
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Microservice that = (Microservice) o;
        return this.hashCode() == that.hashCode() && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + apis.hashCode();
        result = 31 * result + agent.hashCode();
        result = 31 * result + cloud.hashCode();
        return result;
    }
}
