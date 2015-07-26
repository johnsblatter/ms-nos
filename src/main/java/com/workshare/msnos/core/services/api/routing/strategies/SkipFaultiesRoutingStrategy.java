package com.workshare.msnos.core.services.api.routing.strategies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.workshare.msnos.core.services.api.Microservice;
import com.workshare.msnos.core.services.api.RemoteMicroservice;
import com.workshare.msnos.core.services.api.routing.ApiEndpoint;
import com.workshare.msnos.core.services.api.routing.RoutingStrategy;

public class SkipFaultiesRoutingStrategy implements RoutingStrategy {

    @Override
    public List<ApiEndpoint> select(Microservice from, List<ApiEndpoint> apis) {
        final Set<RemoteMicroservice> faulties = new HashSet<RemoteMicroservice>();
        for (ApiEndpoint api : apis) {
            if (api.isFaulty())
                faulties.add(api.service());
        }

        if (faulties.isEmpty())
            return apis;
        
        final List<ApiEndpoint> result = new ArrayList<ApiEndpoint>();
        for (ApiEndpoint api : apis) {
            if (!faulties.contains(api.service()))
                result.add(api);
        }

        return result;
    }

}
