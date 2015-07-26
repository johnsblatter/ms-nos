package com.workshare.msnos.core.services.api.routing;

import java.util.List;

import com.workshare.msnos.core.services.api.Microservice;

public interface RoutingStrategy {
    public List<ApiEndpoint> select(Microservice from, List<ApiEndpoint> apis)
    ;
}
