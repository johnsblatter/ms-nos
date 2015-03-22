package com.workshare.msnos.usvc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateways;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.geo.LocationFactory;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpClientFactory;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.usvc.api.RestApi;


/**
 * The mocking of the HttpClientFactory is built this way because of the 
 * crappy implementation of the Powemock cglib :( [bb]
 * (I knew I should not have used it)
 * @see HttpClientFactory
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Gateways.class, HttpClientFactory.class})
public class MicroserviceLocationTest {

    private static final String SYRACUSE = "24.24.24.24";

    private Cloud cloud;

    @Before
    public void prepare() throws Exception {
        cloud = mock(Cloud.class);
        when(cloud.getRing()).thenReturn(Ring.random());
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, UUID.randomUUID()));
        
        PowerMockito.mockStatic(Gateways.class);
        when(Gateways.allEndpoints()).thenReturn(Collections.<Endpoint>emptySet());
        
        PowerMockito.mockStatic(HttpClientFactory.class);
        HttpClient client = mock(HttpClient.class);
        when(HttpClientFactory.sharedHttpClient()).thenReturn(client );
        when(HttpClientFactory.newHttpClient()).thenReturn(client);
    }

    @Test
    public void shouldRemoteStoreServiceLocationWhenSingleHomed() {

        String host = "24.24.24.24";
        RemoteAgent agent = new RemoteAgent(UUID.randomUUID(), cloud, endpoints(host));
        RemoteMicroservice remote = new RemoteMicroservice("wombats", agent, Collections.<RestApi>emptySet());

        Location expected = LocationFactory.DEFAULT.make(host);
        Location current = remote.getLocation();

        assertEquals(expected, current);
    }

    @Test
    public void shouldRemoteStoreMostPreciseServiceLocationWhenMultiHomed() {

        RemoteAgent agent = new RemoteAgent(UUID.randomUUID(), cloud, multiHomedEndpoints(SYRACUSE));
        RemoteMicroservice remote = new RemoteMicroservice("wombats", agent, Collections.<RestApi>emptySet());

        Location expected = LocationFactory.DEFAULT.make(SYRACUSE);
        Location current = remote.getLocation();

        assertEquals(expected, current);
    }

    @Test
    public void shouldLocalStoreServiceLocationWhenSingleHomed() {

        LocalAgent agent = mock(LocalAgent.class);
        when(agent.getEndpoints()).thenReturn(singleHomedEndpoints(SYRACUSE));
        Microservice micro = new Microservice("wombats", agent);

        Location expected = LocationFactory.DEFAULT.make(SYRACUSE);
        Location current = micro.getLocation();

        assertEquals(expected, current);
    }

    @Test
    public void shouldLocalStoreMostPreciseServiceLocationWhenMultiHomed() {

        LocalAgent agent = mock(LocalAgent.class);
        when(agent.getEndpoints()).thenReturn(multiHomedEndpoints(SYRACUSE));
        Microservice micro = new Microservice("wombats", agent);

        Location expected = LocationFactory.DEFAULT.make(SYRACUSE);
        Location current = micro.getLocation();

        assertEquals(expected, current);
    }

    private Set<Endpoint> multiHomedEndpoints(String city) {
        final String country1 = "31.29.0.0";    // Kyrgyzstan, Asia
        final String city2 = "46.36.195.0";     // Antarctica, Antarctica (no region)

        return endpoints(country1, "10.10.0.1", city2, city);
    }

    private Set<Endpoint> singleHomedEndpoints(String host) {
        return endpoints(host);
    }


    private Set<Endpoint> endpoints(String... hosts) {
        Set<Endpoint> nets = new HashSet<Endpoint>();
        for (String host : hosts) {
            nets.add(new BaseEndpoint(Endpoint.Type.UDP, makeNetwork(host)));
        }
        return nets;
    }

    private Network makeNetwork(String host) {
        byte[] bytes = new byte[4];
        String[] bytesAsString = host.split("\\.");
        for (int i = 0; i < 4; i++) {
            bytes[i] = Integer.valueOf(bytesAsString[i]).byteValue();
        }

        return new Network(bytes, (short) 256);
    }

}
