package com.workshare.msnos.core.services.api.routing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.services.api.LocalMicroservice;
import com.workshare.msnos.core.services.api.routing.ApiEndpoint;
import com.workshare.msnos.core.services.api.routing.RoutingStrategy;
import com.workshare.msnos.core.services.api.routing.strategies.CachingRoutingStrategy;

public class CachingRoutingStrategyTest {

    private LocalMicroservice from;
    
    private List<ApiEndpoint> endpoints;

    private CachingRoutingStrategy strategy;

    private RoutingStrategy delegateStrategy;

    private ApiEndpoint api;

    @Before
    public void setup() {
        from = mock(LocalMicroservice.class);
        delegateStrategy = mock(RoutingStrategy.class);
        
        api = mock(ApiEndpoint.class);
        endpoints = Arrays.asList(api);
        
        strategy = new CachingRoutingStrategy(delegateStrategy);
        strategy.withTimeout(200, TimeUnit.MILLISECONDS);
        
        reinitialize();
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
        reinitialize();

        Thread.sleep(100L);
        
        strategy.select(from, endpoints);
        verifyZeroInteractions(delegateStrategy);

    }
    
    @Test
    public void shouldInvokeUnderlyingStrategyIfExecutedOutsideTimeout() throws Exception {
        strategy.select(from, endpoints);
        verify(delegateStrategy).select(from, endpoints);
        reinitialize();
        
        Thread.sleep(255L);
        
        strategy.select(from, endpoints);
        verify(delegateStrategy).select(from, endpoints);
    }

    @Test
    public void shouldInvokeUnderlyingStrategyIfSelectedApiFaulty() throws Exception {
        strategy.select(from, endpoints);
        verify(delegateStrategy).select(from, endpoints);
        reinitialize();

        when(api.isFaulty()).thenReturn(true);
        Thread.sleep(100L);
        
        strategy.select(from, endpoints);
        verify(delegateStrategy).select(from, endpoints);
    }
    
    public void reinitialize() {
        reset(delegateStrategy);
        when(delegateStrategy.select(from, endpoints)).thenReturn(endpoints);
    }
    
}
