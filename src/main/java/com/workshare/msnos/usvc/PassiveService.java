package com.workshare.msnos.usvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.PassiveAgent;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.usvc.api.RestApi;

public class PassiveService implements IMicroservice {
    private final Microcloud cloud;
    private final String name;
    private final String host;
    private final String healthCheckUri;
    private final int port;

    private final PassiveAgent agent;
    private final Set<RestApi> apis;
    private final Location location;

    public PassiveService(Microcloud cloud, String name, String host, int port, String healthCheckUri) throws IllegalArgumentException {
        this.cloud = cloud;
        this.name = name;
        this.host = host;
        this.port = port;
        this.healthCheckUri = healthCheckUri;
        this.agent = new PassiveAgent(cloud.getCloud(), UUID.randomUUID());
        this.apis = new CopyOnWriteArraySet<RestApi>();
        this.location = Location.computeLocation(host);
    }

    @Override
    public PassiveAgent getAgent() {
        return agent;
    }

    @Override
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
        return agent.getIden().getUUID();
    }

    public void join() throws MsnosException {
        cloud.onJoin(this);
    }

    public void publish(RestApi... newApis) throws MsnosException {
        cloud.publish(this, newApis);
        apis.addAll(Arrays.asList(newApis));
    }
    
    @Override
    public Set<RestApi> getApis() {
        return Collections.unmodifiableSet(apis);
    }

    @Override
    public Location getLocation() {
        return location;
    }
}
