package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.payloads.QnePayload;

import java.io.IOException;
import java.util.*;

public class Microservice {

    private final String name;
    private final List<RestApi> apis;
    private final Agent agent;

    private Cloud cloud;
    private final Map<String, Set<RestApi>> microServices;

    public Microservice(String name) {
        this.name = name;
        this.apis = new ArrayList<RestApi>();
        this.microServices = new HashMap<String, Set<RestApi>>();
        this.agent = new Agent(UUID.randomUUID());
        this.cloud = null;
    }

    Agent getAgent() {
        return agent;
    }

    public String getName() {
        return name;
    }

    public List<RestApi> listApis() {
        return Collections.unmodifiableList(apis);
    }

    public Map<String, Set<RestApi>> getMicroServices() {
        return Collections.unmodifiableMap(microServices);
    }

    public void join(Cloud nimbus) throws IOException {
        agent.join(nimbus);
        this.cloud = nimbus;
        cloud.addListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                process(message);
            }
        });
        Message message = new Message(Message.Type.ENQ, agent.getIden(), cloud.getIden(), 2, false, null);
        agent.send(message);
    }

    public void publish(RestApi api) throws IOException {
        Message message = new Message(Message.Type.QNE, agent.getIden(), cloud.getIden(), 2, false, new QnePayload(name, api));
        agent.send(message);
    }

    private void process(Message message) {
        if (message.getType() == Message.Type.QNE) {
            processQNE(message);
        }
    }

    private void processQNE(Message message) {
        synchronized (microServices) {
            String name = ((QnePayload) message.getData()).getName();
            Set<RestApi> apis = ((QnePayload) message.getData()).getApis();
            if (!microServices.containsKey(name)) {
                microServices.put(name, apis);
            }
        }
    }
}
