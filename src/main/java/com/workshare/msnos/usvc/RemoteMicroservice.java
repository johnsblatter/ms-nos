package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.protocols.ip.Network;

import java.util.HashSet;
import java.util.Set;

public class RemoteMicroservice {

    private Agent agent;
    private String name;
    private Set<RestApi> apis;

    public RemoteMicroservice(String name, Agent agent, Set<RestApi> apis) {
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
                    result.add(api.withHost(network.toString()));
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

    public Agent getAgent() {
        return agent;
    }
}