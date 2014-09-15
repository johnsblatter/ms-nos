package com.workshare.msnos.usvc.api.routing.strategies;

import com.workshare.msnos.usvc.Microservice;
import com.workshare.msnos.usvc.api.routing.ApiEndpoint;
import com.workshare.msnos.usvc.api.routing.RoutingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rhys on 10/09/14.
 */
public class PriorityRoutingStrategy implements RoutingStrategy {

    @Override
    public List<ApiEndpoint> select(Microservice from, List<ApiEndpoint> apis) {
        boolean noPriority = true;
        for (ApiEndpoint api : apis) {
            if (api.priority() != 0 && !api.isFaulty()) {
                noPriority = false;
            }
        }

        if (noPriority)
            return apis;

        int highest = 0;
        for (ApiEndpoint api : apis) {
            if (api.priority() > highest && !api.isFaulty())
                highest = api.priority();
        }

        List<ApiEndpoint> priorityEndpoints = new ArrayList<ApiEndpoint>();
        for (ApiEndpoint api : apis) {
            if (api.priority() == highest && !api.isFaulty())
                priorityEndpoints.add(api);
        }

        return priorityEndpoints;
    }
}
