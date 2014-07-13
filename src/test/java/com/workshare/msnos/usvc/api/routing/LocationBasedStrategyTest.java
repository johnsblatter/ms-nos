package com.workshare.msnos.usvc.api.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.geo.Location.Place;
import com.workshare.msnos.usvc.Microservice;
import com.workshare.msnos.usvc.api.routing.strategies.LocationBasedStrategy;

public class LocationBasedStrategyTest {
    
    private static final Location EUROPE = new Location(continent("EU"), null, null, null);
    private static final Location ITALY = new Location(continent("EU"), country("IT"), null, null);
    private static final Location PARIS = new Location(continent("EU"), country("FR"), region("RS"), city("PA"));
    private static final Location MILAN = new Location(continent("EU"), country("IT"), region("LO"), city("MI"));
    private static final Location TURIN = new Location(continent("EU"), country("IT"), region("PI"), city("TO"));
    private static final Location ASTI  = new Location(continent("EU"), country("IT"), region("PI"), city("AT"));
    private static final Location CUNEO = new Location(continent("EU"), country("IT"), region("PI"), city("CN"));

    private static final Location ASIA = new Location(continent("AS"), null, null, null);
    private static final Location KONG = new Location(continent("AS"), country("HK"), region("HK"), city("HK"));

    @Test
    public void shouldSelectAllWhenNothingInTheSameContinent() {
        Microservice micro = Mockito.mock(Microservice.class);
        when(micro.getLocation()).thenReturn(ASIA);
        
        List<ApiEndpoint> endpoints = makeEndpoints(PARIS, MILAN, TURIN, CUNEO);
        List<ApiEndpoint> result = new LocationBasedStrategy().select(micro, endpoints);
        assertEquals(4, result.size());
    }

    @Test
    public void shouldSelectSameContinent() {
        Microservice micro = Mockito.mock(Microservice.class);
        when(micro.getLocation()).thenReturn(EUROPE);
        
        List<ApiEndpoint> endpoints = makeEndpoints(PARIS, MILAN, KONG, ASIA);
        List<ApiEndpoint> result = new LocationBasedStrategy().select(micro, endpoints);
        assertEquals(2, result.size());
        assertResultContainsLocation(result, PARIS);
        assertResultContainsLocation(result, MILAN);
    }

    @Test
    public void shouldSelectSameCountry() {
        Microservice micro = Mockito.mock(Microservice.class);
        when(micro.getLocation()).thenReturn(ITALY);
        
        List<ApiEndpoint> endpoints = makeEndpoints(PARIS, MILAN, TURIN, ASTI, CUNEO, KONG, ASIA);
        List<ApiEndpoint> result = new LocationBasedStrategy().select(micro, endpoints);
        assertEquals(4, result.size());
        assertResultContainsLocation(result, CUNEO);
        assertResultContainsLocation(result, ASTI);
        assertResultContainsLocation(result, TURIN);
        assertResultContainsLocation(result, MILAN);
    }

    @Test
    public void shouldSelectSameRegion() {
        Microservice micro = Mockito.mock(Microservice.class);
        when(micro.getLocation()).thenReturn(TURIN);
        
        List<ApiEndpoint> endpoints = makeEndpoints(PARIS, MILAN, ASTI, CUNEO, KONG, ASIA);
        List<ApiEndpoint> result = new LocationBasedStrategy().select(micro, endpoints);
        assertEquals(2, result.size());
        assertResultContainsLocation(result, CUNEO);
        assertResultContainsLocation(result, ASTI);
    }


    @Test
    public void shouldSelectSameCity() {
        Microservice micro = Mockito.mock(Microservice.class);
        when(micro.getLocation()).thenReturn(TURIN);
        
        List<ApiEndpoint> endpoints = makeEndpoints(PARIS, MILAN, ASTI, CUNEO, KONG, ASIA, TURIN);
        List<ApiEndpoint> result = new LocationBasedStrategy().select(micro, endpoints);
        assertEquals(1, result.size());
        assertResultContainsLocation(result, TURIN);
    }
    
    private void assertResultContainsLocation(List<ApiEndpoint> result, final Location location) {
        boolean found = false;
        for (ApiEndpoint api : result) {
            if (api.location().equals(location)) {
                found = true;
                break;
            }
        }        
        
        assertTrue("Location "+location+" expected but not found", found);
    }

    private List<ApiEndpoint> makeEndpoints(Location... locations) {
        List<ApiEndpoint> result = new ArrayList<ApiEndpoint>();
        for (Location location : locations) {
            ApiEndpoint endpoint = mock(ApiEndpoint.class);
            when(endpoint.location()).thenReturn(location);
            result.add(endpoint);
        }
        
        return result;
    }

    private static Place continent(String code) {
        return new Place(Place.Type.CONTINENT, code, code);
    }
    
    private static Place country(String code) {
        return new Place(Place.Type.COUNTRY, code, code);
    }

    private static Place region(String code) {
        return new Place(Place.Type.REGION, code, code);
    }

    private static Place city(String code) {
        return new Place(Place.Type.CITY, code, code);
    }
}
