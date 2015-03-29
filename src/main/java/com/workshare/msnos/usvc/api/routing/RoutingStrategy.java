package com.workshare.msnos.usvc.api.routing;

import java.util.List;

import com.workshare.msnos.usvc.IMicroservice;

public interface RoutingStrategy {
    public List<ApiEndpoint> select(IMicroservice from, List<ApiEndpoint> apis)
    ;
}
