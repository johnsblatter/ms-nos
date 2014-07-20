package com.workshare.msnos.usvc.api.routing.strategies;

import java.util.List;

import com.workshare.msnos.usvc.Microservice;
import com.workshare.msnos.usvc.api.routing.ApiEndpoint;
import com.workshare.msnos.usvc.api.routing.RoutingStrategy;

public class CompositeStrategy implements RoutingStrategy {

    private final RoutingStrategy[] strategies;

    public CompositeStrategy (RoutingStrategy... strategies) {
        this.strategies = strategies;
    }
    
    @Override
    public List<ApiEndpoint> select(Microservice from, List<ApiEndpoint> apis) {
        List<ApiEndpoint> result = apis;
        for (RoutingStrategy strategy : strategies) {
            result = strategy.select(from, result);
        }
        
        return result;
    }

}
