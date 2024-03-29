package com.workshare.msnos.usvc.api.routing.strategies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.workshare.msnos.usvc.IMicroservice;
import com.workshare.msnos.usvc.RemoteMicroservice;
import com.workshare.msnos.usvc.api.routing.ApiEndpoint;
import com.workshare.msnos.usvc.api.routing.RoutingStrategy;

public class SkipFaultiesRoutingStrategy implements RoutingStrategy {

    @Override
    public List<ApiEndpoint> select(IMicroservice from, List<ApiEndpoint> apis) {
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
