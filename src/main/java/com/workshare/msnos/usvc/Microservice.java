package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.payloads.QnePayload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Microservice {

    private final String name;
    private final List<RestApi> apis;
    private final Agent agent;

    private Cloud cloud;

    public Microservice(String name) {
        this.name = name;
        this.apis = new ArrayList<RestApi>();
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

    public void join(Cloud nimbus) throws IOException {
        agent.join(nimbus);
        this.cloud = nimbus;
    }

    public void publish(RestApi api) throws IOException {
        Message message = new Message(Message.Type.QNE, agent.getIden(), cloud.getIden(), 2, false, new QnePayload(api));
        agent.send(message);
    }

}
