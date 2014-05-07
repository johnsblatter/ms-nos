package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class Microservice {

    private static final Logger log = LoggerFactory.getLogger("root");

    private final String name;
    private final List<RestApi> apis;
    private final Agent agent;

    private Cloud cloud;
    private final List<RemoteMicroservice> microServices;

    public Microservice(String name) {
        this.name = name;
        this.apis = new ArrayList<RestApi>();
        this.microServices = new ArrayList<RemoteMicroservice>();
        this.agent = new Agent(UUID.randomUUID());
        this.cloud = null;
    }

    public Iden getAgentIden() {
        return agent.getIden();
    }

    public String getName() {
        return name;
    }

    public Set<RestApi> getApis() {
        return Collections.unmodifiableSet(new HashSet<RestApi>(apis));
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
            String name = ((QnePayload) message.getData()).getName();
            Iden iden = message.getFrom();
            Set<RestApi> apis = ((QnePayload) message.getData()).getApis();
            microServices.add(new RemoteMicroservice(iden, name, apis));
        }
    }

    private void processFault(Message message) {
        FltPayload fault = (FltPayload) message.getData();
        synchronized (microServices) {
            for (int i = 0; i < microServices.size(); i++) {
                if (microServices.get(i).getIden().equals(fault.getAbout())) {
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
                    if (remote.getName().contains(name) && rest.getPath().contains(endpoint) && !rest.isFaulty())
                        return rest;
                }
            }
        }
        return null;
    }
}
