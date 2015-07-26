package com.workshare.msnos.core.services.api.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.services.api.LocalMicroservice;
import com.workshare.msnos.core.services.api.RemoteMicroservice;
import com.workshare.msnos.core.services.api.routing.ApiEndpoint;
import com.workshare.msnos.core.services.api.routing.strategies.SkipFaultiesRoutingStrategy;

@SuppressWarnings("unused")
public class SkipFaultiesRoutingStrategyTest {

    private LocalMicroservice from;
    
    private RemoteMicroservice one;
    private RemoteMicroservice two;

    private List<ApiEndpoint> endpoints;

    private SkipFaultiesRoutingStrategy strategy;

    @Before
    public void setup() {
        from = mock(LocalMicroservice.class);

        one = mock(RemoteMicroservice.class);
        two = mock(RemoteMicroservice.class);

        endpoints = new ArrayList<ApiEndpoint>();
        strategy = new SkipFaultiesRoutingStrategy();
    }
    
    @Test
    public void shouldReturnUnchangedListIfNoFaulties() {
        ApiEndpoint oneA = add(endpointOk(one));
        ApiEndpoint oneB = add(endpointOk(one));
        ApiEndpoint twoA = add(endpointOk(two));
        
        List<ApiEndpoint> result = strategy.select(from, endpoints);
       
        assertEquals(3, result.size());
        assertTrue(result.contains(oneA));
        assertTrue(result.contains(oneB));
        assertTrue(result.contains(twoA));
    }

    
    @Test
    public void shouldSkipApisOfDFaulties() {
        ApiEndpoint oneA = add(endpointOk(one));
        ApiEndpoint oneB = add(endpointKO(one));
        ApiEndpoint twoA = add(endpointOk(two));
        
        List<ApiEndpoint> result = strategy.select(from, endpoints);
       
        assertEquals(1, result.size());
        assertTrue(result.contains(twoA));
        
    }

    private ApiEndpoint add(final ApiEndpoint endpoint) {
        endpoints.add(endpoint);
        return endpoint;
    }
    

    public ApiEndpoint endpointKO(RemoteMicroservice micro) {
        ApiEndpoint ep = endpointOk(micro);
        when(ep.isFaulty()).thenReturn(true);
        return ep;
    }
    
    public ApiEndpoint endpointOk(RemoteMicroservice micro) {
        ApiEndpoint endpoint = mock(ApiEndpoint.class);
        when(endpoint.service()).thenReturn(micro);
        return endpoint;
    }
}
