package com.workshare.msnos.usvc;

import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.usvc.api.RestApi;

import java.util.HashSet;
import java.util.Set;

public class RemoteMicroservice {

    private final RemoteAgent agent;
    private final String name;
    private final Set<RestApi> apis;
    private final Location location;

    public RemoteMicroservice(String name, RemoteAgent agent, Set<RestApi> apis) {
        this.name = name;
        this.agent = agent;
        this.apis = ensureHostIsPresent(apis);
        this.location = Location.computeMostPreciseLocation(agent.getEndpoints());
    }

    private Set<RestApi> ensureHostIsPresent(Set<RestApi> apis) {
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

    public void addApis(Set<RestApi> restApis) {
        synchronized (apis) {
            apis.addAll(ensureHostIsPresent(restApis));
        }
    }

    public String getName() {
        return name;
    }

    public Set<RestApi> getApis() {
        return apis;
    }

    public RemoteAgent getAgent() {
        return agent;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "RemoteMicroservice{" +
                "agent=" + agent +
                ", name='" + name + '\'' +
                ", apis=" + apis +
                ", location=" + location +
                '}';
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
        agent.touch();
        for (RestApi rest : getApis()) {
            rest.markWorking();
        }
    }

    public void markFaulty() {
        for (RestApi rest : getApis()) {
            rest.markFaulty();
        }
    }
}