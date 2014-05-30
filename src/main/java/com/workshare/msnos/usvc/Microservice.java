package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Microservice {

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final String name;
    private final LocalAgent agent;
    private Cloud cloud;

    private final List<RestApi> localApis;
    private final List<RemoteMicroservice> microServices;
    private final Map<String, ApiList> remoteApis;

    public Microservice(String name) {
        agent = new LocalAgent(UUID.randomUUID());
        cloud = null;
        this.name = name;

        localApis = new ArrayList<RestApi>();
        microServices = new ArrayList<RemoteMicroservice>();
        remoteApis = new ConcurrentHashMap<String, ApiList>();
    }

    public Set<RestApi> getLocalApis() {
        return new HashSet<RestApi>(localApis);
    }

    public LocalAgent getAgent() {
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
        localApis.addAll(Arrays.asList(api));
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
        agent.send(new Message(Message.Type.QNE, agent.getIden(), cloud.getIden(), 2, false, new QnePayload(name, getLocalApis())));
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
                }
            }
            RemoteMicroservice remote = new RemoteMicroservice(name, remoteAgent, apis);
            microServices.add(remote);
            processRestApis(remote, apis);
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
                    microServices.remove(microServices.get(i));
//             TODO Remove from RemoteApis
                    break;
                }
            }
        }
    }

    public RestApi searchApi(String name, String path) throws Exception {
        String key = name + path;
        ApiList apiList = remoteApis.get(key);
        return apiList.get();
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
        result = 31 * result + localApis.hashCode();
        result = 31 * result + agent.hashCode();
        result = 31 * result + cloud.hashCode();
        return result;
    }
}
