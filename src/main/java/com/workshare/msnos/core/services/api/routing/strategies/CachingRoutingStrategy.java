package com.workshare.msnos.core.services.api.routing.strategies;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.services.api.Microservice;
import com.workshare.msnos.core.services.api.routing.ApiEndpoint;
import com.workshare.msnos.core.services.api.routing.RoutingStrategy;

public class CachingRoutingStrategy implements RoutingStrategy {
    private static final Logger log = LoggerFactory.getLogger(CachingRoutingStrategy.class);

    public static final String SYSP_TIMEOUT = "com.ws.nsnos.usvc.api.routing.strategy.caching.timeout";

    private final RoutingStrategy delegate;

    private long timeout;
    private List<ApiEndpoint> result;

    private long lastrun;
    private long lastend;

    public CachingRoutingStrategy(RoutingStrategy delegate) {
        this.delegate = delegate;
        this.timeout = getDefaultTimeout();
        this.result = Collections.emptyList();

        this.lastrun = System.currentTimeMillis();
        this.lastend = lastrun - 1;
    }

    private static long getDefaultTimeout() {
        return Long.getLong(SYSP_TIMEOUT, 250l);
    }

    private void reset() {
        this.lastrun = System.currentTimeMillis();
        this.lastend = System.currentTimeMillis() + timeout;
    }

    @Override
    public List<ApiEndpoint> select(Microservice from, List<ApiEndpoint> apis) {
        if (timeout == 0L) {
            return delegate.select(from, apis);
        }

        if (System.currentTimeMillis() > lastend || isFaulty(result)) {
            reset();
            result = delegate.select(from, apis);
        }

        return result;
    }

    public CachingRoutingStrategy withTimeout(int duration, TimeUnit unit) {
        this.timeout = TimeUnit.MILLISECONDS.convert(duration, unit);
        return this;
    }

    private boolean isFaulty(List<ApiEndpoint> eps) {
        for (ApiEndpoint ep : eps) {
            if (ep.isFaulty()) {
                log.debug("Current endpoints list is faulty on endpoint {}", ep);
                return true;
            }
        }
        
        return false;
    }
}
