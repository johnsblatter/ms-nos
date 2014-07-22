package com.workshare.msnos.usvc.api.routing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.usvc.Microservice;
import com.workshare.msnos.usvc.api.routing.strategies.CachingRoutingStrategy;

public class CachingRoutingStrategyTest {

    private Microservice from;
    
    private List<ApiEndpoint> endpoints;

    private CachingRoutingStrategy strategy;

    private RoutingStrategy delegateStrategy;

    @Before
    public void setup() {
        from = mock(Microservice.class);
        delegateStrategy = mock(RoutingStrategy.class);
        
        endpoints = new ArrayList<ApiEndpoint>();
        strategy = new CachingRoutingStrategy(delegateStrategy);
        strategy.withTimeout(200, TimeUnit.MILLISECONDS);
    }
    
    @Test
    public void shouldInvokeUnderlyingStrategy() {
        strategy.select(from, endpoints);
        verify(delegateStrategy).select(from, endpoints);
    }

    
    @Test
    public void shouldNotInvokeUnderlyingStrategyIfExecutedWithinTimeout() throws Exception {
        strategy.select(from, endpoints);
        verify(delegateStrategy).select(from, endpoints);
        
        Thread.sleep(100L);
        
        strategy.select(from, endpoints);
        verifyZeroInteractions(delegateStrategy);

    }
    
    @Test
    public void shouldInvokeUnderlyingStrategyIfExecutedOutsideTimeout() throws Exception {
        strategy.select(from, endpoints);
        verify(delegateStrategy).select(from, endpoints);
        
        reset(delegateStrategy);
        Thread.sleep(255L);
        
        strategy.select(from, endpoints);
        verify(delegateStrategy).select(from, endpoints);
    }
    
}
