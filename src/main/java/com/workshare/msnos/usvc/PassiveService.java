package com.workshare.msnos.usvc;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.PassiveAgent;
import com.workshare.msnos.usvc.api.RestApi;

public class PassiveService {
    private final Microcloud cloud;
    private final String name;
    private final String host;
    private final String healthCheckUri;
    private final int port;

    private final UUID uuid;
    private final PassiveAgent agent;
    private final List<RestApi> passiveApis;

    public PassiveService(Microcloud cloud, String name, String host, int port, String healthCheckUri) throws IllegalArgumentException {
        this.cloud = cloud;
        this.name = name;
        this.host = host;
        this.port = port;
        this.healthCheckUri = healthCheckUri;
        this.uuid = UUID.randomUUID();
        this.agent = new PassiveAgent(cloud.getCloud(), uuid);
        this.passiveApis = new CopyOnWriteArrayList<RestApi>();
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
        cloud.onJoin(this);
    }

    public void publish(RestApi... apis) throws MsnosException {
        cloud.publish(this, apis);
    }
    
    public List<RestApi> getPassiveApis() {
        return passiveApis;
    }


}
