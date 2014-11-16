package com.workshare.msnos.usvc;

import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RemoteMicroservice implements IMicroService {

    private final RemoteAgent agent;
    private final String name;
    private final Set<RestApi> apis;
    private final Location location;
    private final AtomicBoolean faulty;
    private final AtomicLong lastUpdated;
    private final AtomicLong lastChecked;
    
    public RemoteMicroservice(String name, RemoteAgent agent, Set<RestApi> apis) {
        this.name = name;
        this.agent = agent;
        this.apis = ensureHostIsPresent(agent, apis);
        this.location = Location.computeMostPreciseLocation(agent.getEndpoints());
        this.faulty = new AtomicBoolean(false);
        this.lastUpdated = new AtomicLong(SystemTime.asMillis());
        this.lastChecked = new AtomicLong(SystemTime.asMillis());
    }

    private static Set<RestApi> ensureHostIsPresent(RemoteAgent agent, Set<RestApi> apis) {
        Set<RestApi> result = new HashSet<RestApi>();
        for (RestApi api : apis) {
            if (api.getHost() == null || api.getHost().isEmpty()) {
                for (Endpoint endpoint : agent.getEndpoints()) {
                    Network network = endpoint.getNetwork();
                    result.add(api.onHost(network.getHostString()));
                }
            } else {
                result.add(api);
            }
        }
        return result;
    }

    protected void setApis(Set<RestApi> restApis) {
        lastUpdated.set(SystemTime.asMillis());
        synchronized (apis) {
            apis.addAll(ensureHostIsPresent(agent, restApis));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<RestApi> getApis() {
        synchronized (apis) {
            return Collections.unmodifiableSet(apis);
        }
    }
    @Override
    public RemoteAgent getAgent() {
        return agent;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("name: '%s', apis: %s, location: %s", name, apis, location);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteMicroservice that = (RemoteMicroservice) o;
        return this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + agent.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public void markWorking() {
        lastChecked.set(SystemTime.asMillis());
        faulty.set(false);
        agent.touch();
        for (RestApi rest : getApis()) {
            rest.markWorking();
        }
    }

    public void markFaulty() {
        lastChecked.set(SystemTime.asMillis());
        faulty.set(true);
        for (RestApi rest : getApis()) {
            rest.markFaulty();
        }
    }
    
    public boolean isFaulty() {
        return faulty.get();
    }

    public long getLastUpdated() {
        return lastUpdated.get();
    }

    public long getLastChecked() {
        return lastChecked.get();
    }
}