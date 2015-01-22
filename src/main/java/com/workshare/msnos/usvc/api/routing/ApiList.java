package com.workshare.msnos.usvc.api.routing;

import com.workshare.msnos.core.geo.LocationFactory;
import com.workshare.msnos.usvc.Microservice;
import com.workshare.msnos.usvc.RemoteMicroservice;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.strategies.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ApiList {

    private static Logger log = LoggerFactory.getLogger(ApiList.class);

    private volatile List<ApiEndpoint> endpoints;
    private volatile RestApi affinite;

    transient private final RoutingStrategy routing;
    transient private final LocationFactory locations;

    transient private final Lock addRemoveLock = new ReentrantLock();

    public ApiList() {
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
        } catch (Throwable ex) {
            log.warn("Unexpected error selecting API using round robin", ex);
            res = endpoints.size() > 0 ? endpoints.get(0) : null;
        }
        return res == null ? null : res.api();
    }

    static RoutingStrategy defaultRoutingStrategy() {
        List<RoutingStrategy> strategies = new ArrayList<RoutingStrategy>(Arrays.asList(new SkipFaultiesRoutingStrategy(), new LocationBasedStrategy(), new RoundRobinRoutingStrategy()));
        if (PriorityRoutingStrategy.isEnabled()) {
            strategies.add(1, new PriorityRoutingStrategy());
        }

        final CompositeStrategy composite = new CompositeStrategy(strategies.toArray(new RoutingStrategy[strategies.size()]));
        return new CachingRoutingStrategy(composite);
    }
}
