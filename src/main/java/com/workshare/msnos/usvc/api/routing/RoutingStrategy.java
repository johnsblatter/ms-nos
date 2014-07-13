package com.workshare.msnos.usvc.api.routing;

import java.util.List;

import com.workshare.msnos.usvc.Microservice;

public interface RoutingStrategy {
    public List<ApiEndpoint> select(Microservice from, List<ApiEndpoint> apis)
    ;
}
