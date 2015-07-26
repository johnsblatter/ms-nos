package com.workshare.msnos.core.services.api.routing;

import com.workshare.msnos.core.services.api.LocalMicroservice;
import com.workshare.msnos.core.services.api.routing.ApiEndpoint;
import com.workshare.msnos.core.services.api.routing.strategies.PriorityRoutingStrategy;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PriorityRoutingStrategyTest {

    public static final LocalMicroservice MICRO = Mockito.mock(LocalMicroservice.class);
    private List<ApiEndpoint> endpoints;

    @Test
    public void shouldSelectBasedOnPriority() throws Exception {
        endpoints = Arrays.asList(lowPriorityEndpoint(), lowPriorityEndpoint(), endpointWithPriority(5));
        List<ApiEndpoint> result = priorityEndpointSelect(MICRO, endpoints);
        assertEquals(1, result.size());
    }

    @Test
    public void shouldReturnALLHighPriorityEndpoints() throws Exception {
        endpoints = Arrays.asList(endpointWithPriority(5), endpointWithPriority(5), endpointWithPriority(5));
        List<ApiEndpoint> result = priorityEndpointSelect(MICRO, endpoints);
        assertEquals(3, result.size());
    }

    @Test
    public void shouldReturnOriginalListIfNoHighPriorityEndpoints() throws Exception {
        endpoints = Arrays.asList(lowPriorityEndpoint(), lowPriorityEndpoint(), lowPriorityEndpoint());
        List<ApiEndpoint> result = priorityEndpointSelect(MICRO, endpoints);
        assertEquals(endpoints, result);
    }

    @Test
    public void shouldSelectHiPriorityEndpoints() throws Exception {
        endpoints = Arrays.asList(endpointWithPriority(5), endpointWithPriority(5), endpointWithPriority(4), endpointWithPriority(3));
        List<ApiEndpoint> result = priorityEndpointSelect(MICRO, endpoints);
        assertEquals(2, result.size());
    }

    @Test
    public void shouldDisregardHiLevelFaultyEndpoints() throws Exception {
        endpoints = Arrays.asList(endpointWithPriorityAndisFaulty(5, true), endpointWithPriorityAndisFaulty(5, true), endpointWithPriority(4), endpointWithPriority(3));
        List<ApiEndpoint> result = priorityEndpointSelect(MICRO, endpoints);
        assertEquals(1, result.size());
    }

    private List<ApiEndpoint> priorityEndpointSelect(LocalMicroservice microservice, List<ApiEndpoint> endpoints) {
        return new PriorityRoutingStrategy().select(microservice, endpoints);
    }

    private ApiEndpoint lowPriorityEndpoint() {
        ApiEndpoint endpoint = mock(ApiEndpoint.class);
        when(endpoint.priority()).thenReturn(0);
        return endpoint;
    }

    private ApiEndpoint endpointWithPriority(int priority) {
        return endpointWithPriorityAndisFaulty(priority, false);
    }

    private ApiEndpoint endpointWithPriorityAndisFaulty(int priority, boolean faulty) {
        ApiEndpoint endpoint = mock(ApiEndpoint.class);
        when(endpoint.priority()).thenReturn(priority);
        when(endpoint.isFaulty()).thenReturn(faulty);
        return endpoint;
    }
}