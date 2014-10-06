package com.workshare.msnos.usvc;

import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.PassiveAgent;
import com.workshare.msnos.usvc.api.RestApi;

import java.util.UUID;

public class PassiveService {
    private final Microservice microservice;
    private final String name;
    private final String host;
    private final String healthCheckUri;
    private final int port;

    private final UUID uuid;
    private final PassiveAgent agent;

    public PassiveService(Microservice microservice, UUID cloudUuid, String name, String host, String healthCheckUri, int port) throws IllegalArgumentException {
        UUID joinedCloudUUID = getJoinedCloudUUID(microservice);

        if (joinedCloudUUID == null)
            throw new IllegalArgumentException("Microservice not currently joined to a cloud, passive service creation refused! ");

        if (!cloudUuid.equals(joinedCloudUUID))
            throw new IllegalArgumentException("Passive microservice trying to join different cloud than microservice's joined cloud! ");

        this.microservice = microservice;
        this.name = name;
        this.host = host;
        this.healthCheckUri = healthCheckUri;
        this.port = port;

        uuid = UUID.randomUUID();
        agent = new PassiveAgent(microservice.getAgent().getCloud(), uuid);
    }

    public PassiveAgent getAgent() {
        return agent;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getHealthCheckUri() {
        return healthCheckUri;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void join() throws MsnosException {
        microservice.passiveJoin(this);
    }

    public void publish(RestApi... apis) throws MsnosException {
        microservice.passivePublish(this, apis);
    }

    private UUID getJoinedCloudUUID(Microservice microservice) {
        if (microservice.getAgent().getCloud() == null)
            return null;
        else
            return microservice.getAgent().getCloud().getIden().getUUID();
    }
}
