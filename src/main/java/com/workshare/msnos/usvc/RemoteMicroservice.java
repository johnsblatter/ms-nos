package com.workshare.msnos.usvc;

import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.protocols.ip.Network;

import java.util.HashSet;
import java.util.Set;

public class RemoteMicroservice {

    private RemoteAgent agent;
    private String name;
    private Set<RestApi> apis;

    public RemoteMicroservice(String name, RemoteAgent agent, Set<RestApi> apis) {
        this.name = name;
        this.apis = apis;
        this.agent = agent;
        this.apis = checkApiHosts(this.apis);
    }

    private Set<RestApi> checkApiHosts(Set<RestApi> apis) {
        Set<RestApi> result = new HashSet<RestApi>();
        for (RestApi api : apis) {
            if (api.getHost() == null || api.getHost().isEmpty()) {
                for (Network network : agent.getHosts()) {
                    result.add(api.host(network.toString()));
                }
            } else {
                result.add(api);
            }
        }
        return result;
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
        result = 31 * result + apis.hashCode();
        result = 31 * result + agent.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}