package com.workshare.msnos.core.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.Test;
import org.mockito.Mockito;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.OmniResponse;

public class LocationFactoryTest {

    private static final String PUBLIC_IP = "25.25.25.25";
    private static final String PRIVATE_IP = "192.168.0.1";

    private DatabaseReader database;
    private LocationFactory factory;

    @Test
    public void shouldLocationBeUnknownIfDatabaseFarts() throws Exception {
        prepareMockConfig();
        when(database.omni(any(InetAddress.class))).thenThrow(new NullPointerException("boom!"));
        
        final Location result = factory.make(PUBLIC_IP);
        assertSame(Location.UNKNOWN, result);
    }
    
    @Test
    public void shouldLocationBeUnknownIfIPPrivate() throws Exception {
        prepareRealConfig();
        
        final Location result = factory.make(PRIVATE_IP);
        assertSame(Location.UNKNOWN, result);
    }
    
    @Test
    public void shouldAddCorrectLocationbasedOnHostIP() throws Exception {
        prepareRealConfig();
        
        final Location result = factory.make(PUBLIC_IP);
        final Location expected = new Location(response(PUBLIC_IP));
        assertEquals(expected, result);

    }

    private OmniResponse response(String ip) throws IOException, GeoIp2Exception {
        return new OfflineLocationFactory().database().omni(InetAddress.getByName(ip));
    }
    
    private void prepareMockConfig() {
        database = Mockito.mock(DatabaseReader.class);
        factory = new OfflineLocationFactory(database);
    }
    
    private void prepareRealConfig() throws Exception {
        factory = LocationFactory.DEFAULT;
        database = null;
    }
    

}
