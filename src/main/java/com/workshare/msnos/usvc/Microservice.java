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
    private final Map<Iden, Set<RestApi>> microServices;

    public Microservice(String name) {
        this.name = name;
        this.apis = new ArrayList<RestApi>();
        this.microServices = new HashMap<Iden, Set<RestApi>>();
        this.agent = new Agent(UUID.randomUUID());
        this.cloud = null;
    }

    public Agent getAgent() {
        return agent;
    }

    public String getName() {
        return name;
    }

    public List<RestApi> listApis() {
        return Collections.unmodifiableList(apis);
    }

    public Map<Iden, Set<RestApi>> getMicroServices() {
        return Collections.unmodifiableMap(microServices);
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

    public void publish(RestApi api) throws IOException {
        Message message = new Message(Message.Type.QNE, agent.getIden(), cloud.getIden(), 2, false, new QnePayload(name, api));
        agent.send(message);
    }

    private void process(Message message) throws IOException {
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

    private void processENQ() throws IOException {
        agent.send(new Message(Message.Type.QNE, agent.getIden(), cloud.getIden(), 2, false, new QnePayload(name, new HashSet<RestApi>(apis))));
    }

    private void processQNE(Message message) {
        synchronized (microServices) {
            Iden iden = message.getFrom();
            if (!iden.equals(agent.getIden())) {
                Set<RestApi> apis = ((QnePayload) message.getData()).getApis();
                if (!microServices.containsKey(iden)) {
                    microServices.put(iden, apis);
                }
            }
        }
    }

    private void processFault(Message message) {
        FltPayload fault = (FltPayload) message.getData();
        synchronized (microServices) {
            microServices.remove(fault.getAbout());
        }

    }

}
