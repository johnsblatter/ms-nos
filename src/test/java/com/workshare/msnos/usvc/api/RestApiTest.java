package com.workshare.msnos.usvc.api;

import static org.junit.Assert.*;

import org.junit.Test;

import com.workshare.msnos.core.geo.Location;

public class RestApiTest {

    @Test
    public void shouldLocationBeUnknownWhenIpNotSpecified() {
        RestApi api = new RestApi("name", "path", 1234);
        assertEquals(api.getLocation(), Location.UNKNOWN);
    }

    @Test
    public void shouldCalculateLocationBasedOnHostIp() {
        final String ipAsString = "25.25.25.25";
        
        RestApi api = new RestApi("name", "path", 1234, ipAsString);
        assertEquals(api.getLocation(), Location.UNKNOWN);
    }
}
