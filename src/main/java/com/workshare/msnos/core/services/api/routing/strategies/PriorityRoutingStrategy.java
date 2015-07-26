package com.workshare.msnos.core.services.api.routing.strategies;

import com.workshare.msnos.core.services.api.Microservice;
import com.workshare.msnos.core.services.api.routing.ApiEndpoint;
import com.workshare.msnos.core.services.api.routing.RoutingStrategy;

import java.util.ArrayList;
import java.util.List;

public class PriorityRoutingStrategy implements RoutingStrategy {

    public static final String SYSP_PRIORITY_ENABLED = "com.ws.nsnos.usvc.priority.enabled";
    public static final String SYSP_PRIORITY_DEFAULT_LEVEL = "com.ws.nsnos.usvc.priority.default.level";

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
    
    public static boolean isEnabled() {
        return Boolean.getBoolean(SYSP_PRIORITY_ENABLED) || getDefaultLevel() != null;
    }

    public static Integer getDefaultLevel() {
        return Integer.getInteger(SYSP_PRIORITY_DEFAULT_LEVEL);
    }
}
