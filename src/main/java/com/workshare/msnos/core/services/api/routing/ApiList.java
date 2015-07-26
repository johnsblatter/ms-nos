package com.workshare.msnos.core.services.api.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.services.api.Microservice;
import com.workshare.msnos.core.services.api.RemoteMicroservice;
import com.workshare.msnos.core.services.api.RestApi;
import com.workshare.msnos.core.services.api.routing.strategies.CachingRoutingStrategy;
import com.workshare.msnos.core.services.api.routing.strategies.CompositeStrategy;
import com.workshare.msnos.core.services.api.routing.strategies.LocationBasedStrategy;
import com.workshare.msnos.core.services.api.routing.strategies.PriorityRoutingStrategy;
import com.workshare.msnos.core.services.api.routing.strategies.RoundRobinRoutingStrategy;
import com.workshare.msnos.core.services.api.routing.strategies.SkipFaultiesRoutingStrategy;

public class ApiList {

    private static Logger log = LoggerFactory.getLogger(ApiList.class);

    private volatile List<ApiEndpoint> endpointsList;
    private volatile RestApi affinite;

    transient private final RoutingStrategy routing;

    transient private final Lock addRemoveLock = new ReentrantLock();

    public ApiList() {
        this(defaultRoutingStrategy());
    }

    public ApiList(RoutingStrategy routingStrategy) {
        this.endpointsList = new ArrayList<ApiEndpoint>();
        this.routing = routingStrategy;
    }

    public void add(RemoteMicroservice remote, RestApi rest) {
        addRemoveLock.lock();
        try {
            LinkedHashSet<ApiEndpoint> newEndpoints = new LinkedHashSet<ApiEndpoint>(endpointsList);
            newEndpoints.add(new ApiEndpoint(remote, rest));
            endpointsList = new ArrayList<ApiEndpoint>(newEndpoints);
        } finally {
            addRemoveLock.unlock();
        }
    }

    public void remove(RemoteMicroservice toRemove) {
        addRemoveLock.lock();
        try {
            List<ApiEndpoint> newEndpoints = new ArrayList<ApiEndpoint>(endpointsList);
            for (int i = 0; i < newEndpoints.size(); i++) {
                final ApiEndpoint endpoint = newEndpoints.get(i);
                if (endpoint.service().equals(toRemove)) {
                    newEndpoints.remove(i);
                    endpoint.api().markFaulty();
                    if (affinite == endpoint.api()) {
                        affinite = null;
                    }
                    
                    break;
                }
            }
            endpointsList = newEndpoints;
        } finally {
            addRemoveLock.unlock();
        }
    }

    public int size() {
        return endpointsList.size();
    }

    // TODO FIXME this need to be changed for performance reason 
    // as we are creating a new list every time
    public List<RestApi> getApis() {
        List<RestApi> result = new ArrayList<RestApi>();
        for (ApiEndpoint api : endpointsList) {
            result.add(api.api());
        }
        return result;
    }

    public List<ApiEndpoint> getEndpoints() {
        return Collections.unmodifiableList(endpointsList);
    }

    public RestApi get(Microservice from) {
        if (endpointsList.size() == 0)
            return null;

        if (affinite != null && !affinite.isFaulty()) {
            return affinite;
        }
        
        RestApi result = getUsingStrategies(from);

        affinite = null;
        if (result != null && result.hasAffinity()) {
            affinite = result;
        }
        
        return result;
    }

    private RestApi getUsingStrategies(Microservice from) {
        ApiEndpoint res;
        try {
            res = routing.select(from, endpointsList).get(0);
        } catch (Throwable ex) {
            log.warn("Unexpected error selecting API", ex);
            res = endpointsList.size() > 0 ? endpointsList.get(0) : null;
        }
        return res == null ? null : res.api();
    }
    

    @Override
    public String toString() {
        return "endpoints="+this.endpointsList+", affinite="+affinite+",hashcode="+super.hashCode();
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
