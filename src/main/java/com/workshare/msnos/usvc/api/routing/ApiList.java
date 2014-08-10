package com.workshare.msnos.usvc.api.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.geo.LocationFactory;
import com.workshare.msnos.usvc.Microservice;
import com.workshare.msnos.usvc.RemoteMicroservice;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.strategies.CachingRoutingStrategy;
import com.workshare.msnos.usvc.api.routing.strategies.CompositeStrategy;
import com.workshare.msnos.usvc.api.routing.strategies.LocationBasedStrategy;
import com.workshare.msnos.usvc.api.routing.strategies.RoundRobinRoutingStrategy;

public class ApiList {

    private static Logger log = LoggerFactory.getLogger(ApiList.class);

    private final Lock addRemoveLock = new ReentrantLock();

    private volatile List<ApiEndpoint> endpoints;
    private volatile RestApi affinite;

    private final RoutingStrategy routing;
    private final LocationFactory locations;

    public ApiList()  {
        this(defaultRoutingStrategy(), LocationFactory.DEFAULT);
    }

    public ApiList(RoutingStrategy routingStrategy, LocationFactory locations) {
        this.endpoints = new ArrayList<ApiEndpoint>();
        this.routing = routingStrategy;
        this.locations = locations;
    }

    public void add(RemoteMicroservice remote, RestApi rest) {
        addRemoveLock.lock();
        try {
            List<ApiEndpoint> newEndpoints = new ArrayList<ApiEndpoint>(endpoints);
            newEndpoints.add(new ApiEndpoint(remote, rest, locations.make(rest.getHost())));
            endpoints = newEndpoints;
        } finally {
            addRemoveLock.unlock();
        }
    }

    public void remove(RemoteMicroservice toRemove) {
        addRemoveLock.lock();
        try {
            List<ApiEndpoint> newEndpoints = new ArrayList<ApiEndpoint>(endpoints);
            for (int i = 0; i < newEndpoints.size(); i++) {
                if (newEndpoints.get(i).service().equals(toRemove)) {
                    newEndpoints.remove(i);
                    break;
                }
            }
            endpoints = newEndpoints;
        } finally {
            addRemoveLock.unlock();
        }
    }

    public List<RestApi> getApis() {
        List<RestApi> result = new ArrayList<RestApi>();
        for (ApiEndpoint api : endpoints) {
            result.add(api.api());
        }
        return result;
    }

    public List<ApiEndpoint> getEndpoints() {
        return Collections.unmodifiableList(endpoints);
    }

    public RestApi get(Microservice from) {
        if (endpoints.size() == 0)
            return null;

        if (affinite != null && !affinite.isFaulty())
            return affinite;

        RestApi result = getUsingStrategies(from);

        if (result != null && result.hasAffinity())
            affinite = result;

        return result;
    }

    private RestApi getUsingStrategies(Microservice from) {
        ApiEndpoint res;
        try {
            res = routing.select(from, endpoints).get(0);
        } catch (Throwable justInCaseSizeChanged) {
            log.warn("Unexpected error selecting API using round robin - "+toString(justInCaseSizeChanged));
            res = endpoints.size() > 0 ? endpoints.get(0) : null;
        }
        return res == null ? null : res.api();
    }
    
    private String toString(Throwable ex) {
        if (ex.getMessage() == null)
            return ex.getClass().getSimpleName();
        else
            return ex.getClass().getSimpleName()+": "+ex.getMessage();
    }

    static RoutingStrategy defaultRoutingStrategy() {
        final CompositeStrategy composite = new CompositeStrategy(new LocationBasedStrategy(), new RoundRobinRoutingStrategy());
        return new CachingRoutingStrategy(composite);
    }
}
