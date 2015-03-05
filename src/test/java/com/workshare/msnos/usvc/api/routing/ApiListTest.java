package com.workshare.msnos.usvc.api.routing;

import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.geo.LocationFactory;
import com.workshare.msnos.core.geo.OfflineLocationFactory;
import com.workshare.msnos.usvc.Microservice;
import com.workshare.msnos.usvc.RemoteMicroservice;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.strategies.CachingRoutingStrategy;
import com.workshare.msnos.usvc.api.routing.strategies.PriorityRoutingStrategy;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ApiListTest {

    private ApiList apiList;
    private Microservice svc;

    private LocationFactory locations = LocationFactory.DEFAULT;
    private RoutingStrategy routing = ApiList.defaultRoutingStrategy();

    @BeforeClass
    public static void disableCaching() {
        System.setProperty(CachingRoutingStrategy.SYSP_TIMEOUT, "0");
    }

    @Before
    public void preparation() {
        disablePriorityStrategy();
        
        this.apiList = null;
        this.locations = mock(OfflineLocationFactory.class);
        this.svc = Mockito.mock(Microservice.class);
        when(svc.getLocation()).thenReturn(Location.UNKNOWN);
    }

    @After
    public void tearDown() throws Exception {
        disablePriorityStrategy();
    }

    private void disablePriorityStrategy() {
        System.clearProperty(PriorityRoutingStrategy.SYSP_PRIORITY_ENABLED);
        System.clearProperty(PriorityRoutingStrategy.SYSP_PRIORITY_DEFAULT_LEVEL);
    }


    @Test
    public void shouldStoreOneApi() {
        RestApi rest = newRestApi("alfa");
        RemoteMicroservice remote = newtRemoteMicroservice();

        apiList().add(remote, rest);

        assertEquals(rest, apiList().get(svc));
        assertEquals(rest, apiList().get(svc));
    }

    @Test
    public void shouldNotReturnFaultyApis() {
        RestApi rest = newRestApi("alfa");
        RemoteMicroservice remote = newtRemoteMicroservice();
        apiList().add(remote, rest);

        markAsFaulty(rest);
        
        assertNull(apiList().get(svc));
    }

    @Test
    public void shouldSelectApisUsingRoundRobin() {
        RestApi alfa = newRestApi("alfa");
        RestApi beta = newRestApi("beta");

        RemoteMicroservice remote = newtRemoteMicroservice();
        apiList().add(remote, alfa);
        apiList().add(remote, beta);

        assertEquals(alfa, apiList().get(svc));
        assertEquals(beta, apiList().get(svc));
        assertEquals(alfa, apiList().get(svc));
    }

    @Test
    public void shouldSelectApisUsingRoundRobinButSkippingFailingRemoteServices() {
        RestApi oneAlfa = newRestApi("one.alfa");
        RestApi oneBeta = newRestApi("one,beta");
        RestApi twoAlfa = newRestApi("two.alfa");

        RemoteMicroservice one = newtRemoteMicroservice();
        RemoteMicroservice two = newtRemoteMicroservice();
        apiList().add(one, oneAlfa);
        apiList().add(one, oneBeta);
        apiList().add(two, twoAlfa);

        markAsFaulty(oneAlfa);

        assertEquals(twoAlfa, apiList().get(svc));
    }

    @Test
    public void shouldHonourAffinity() {
        RestApi alfa = newRestApi("alfa");
        RestApi beta = newRestApiWithAffinity("beta");

        RemoteMicroservice remote = newtRemoteMicroservice();
        apiList().add(remote, alfa);
        apiList().add(remote, beta);

        assertEquals(alfa, apiList().get(svc));
        assertEquals(beta, apiList().get(svc));
        assertEquals(beta, apiList().get(svc));
    }

    @Test
    public void shouldRemoveAffinityWhenFaulty() {
        RestApi alfa = newRestApi("alfa");
        RestApi beta = newRestApiWithAffinity("beta");
        RestApi thre = newRestApi("thre");

        apiList().add(newtRemoteMicroservice(), alfa);
        apiList().add(newtRemoteMicroservice(), beta);
        apiList().add(newtRemoteMicroservice(), thre);

        markAsFaulty(beta);

        assertEquals(alfa, apiList().get(svc));
        assertEquals(thre, apiList().get(svc));
        assertEquals(alfa, apiList().get(svc));
    }

    @Test
    public void shouldMapNewAffinityWhenOriginalFaulty() {
        RestApi alfa = newRestApiWithAffinity("alfa");
        RestApi beta = newRestApiWithAffinity("beta");
        RestApi thre = newRestApi("thre");

        apiList().add(newtRemoteMicroservice(), alfa);
        apiList().add(newtRemoteMicroservice(), beta);
        apiList().add(newtRemoteMicroservice(), thre);

        markAsFaulty(alfa);

        assertEquals(beta, apiList().get(svc));
        assertEquals(beta, apiList().get(svc));

    }

    @Test
    public void shouldNOTRemapToNewlyWorkingAffinityAfterFaulty() {
        RestApi alfa = newRestApiWithAffinity("alfa");
        RestApi beta = newRestApiWithAffinity("beta");
        RestApi thre = newRestApi("thre");

        apiList().add(newtRemoteMicroservice(), alfa);
        apiList().add(newtRemoteMicroservice(), beta);
        apiList().add(newtRemoteMicroservice(), thre);

        markAsFaulty(alfa);

        assertEquals(beta, apiList().get(svc));
        assertEquals(beta, apiList().get(svc));

        markAsWorking(alfa);

        assertEquals(beta, apiList().get(svc));
        assertEquals(beta, apiList().get(svc));
    }

    @Test
    public void shouldAddLocationToApiEndpoint() {
        locations = mock(OfflineLocationFactory.class);
        Location location = mock(Location.class);
        when(locations.make("1.1.1.1")).thenReturn(location);

        apiList().add(newtRemoteMicroservice(), newRestApiWithHost("alfa", "1.1.1.1"));

        ApiEndpoint ep = apiList().getEndpoints().get(0);
        assertEquals(location, ep.location());
    }

    @Test
    public void shouldInvokeUnderlyingStrategy() {
        routing = mock(RoutingStrategy.class);
        final RestApi alfa = newRestApi("alfa");
        apiList().add(newtRemoteMicroservice(), alfa);

        when(routing.select(svc, apiList.getEndpoints())).thenReturn(apiList.getEndpoints());
        apiList().get(svc);

        ArgumentCaptor<List> apis = ArgumentCaptor.forClass(List.class);
        verify(routing).select(eq(svc), apis.capture());
        assertEquals(1, apis.getValue().size());
        assertEquals(((ApiEndpoint) apis.getValue().get(0)).api(), alfa);
    }

    @Test
    public void shouldInvokePriorityStrategyWhenHighPriorityEngaged() throws Exception {
        System.setProperty(PriorityRoutingStrategy.SYSP_PRIORITY_ENABLED, "true");
        routing = ApiList.defaultRoutingStrategy();

        final RestApi alfa = newRestApiWithHighPriority("alfa");
        final RestApi beta = newRestApi("beta");
        apiList().add(newtRemoteMicroservice(), alfa);
        apiList().add(newtRemoteMicroservice(), beta);

        assertEquals(alfa, apiList().get(svc));
        assertEquals(alfa, apiList().get(svc));
        assertEquals(alfa, apiList().get(svc));
    }

    @Test
    public void shouldNOTInvokePriorityStrategyWhenHighPriorityNOTEngaged() throws Exception {
        final RestApi alfa = newRestApiWithHighPriority("alfa");
        final RestApi beta = newRestApi("beta");
        apiList().add(newtRemoteMicroservice(), alfa);
        apiList().add(newtRemoteMicroservice(), beta);

        assertEquals(alfa, apiList().get(svc));
        assertEquals(beta, apiList().get(svc));
        assertEquals(alfa, apiList().get(svc));
    }

    private RemoteMicroservice newtRemoteMicroservice() {
        final RemoteMicroservice micro = Mockito.mock(RemoteMicroservice.class);
        Mockito.when(micro.getName()).thenReturn("usvc");
        Mockito.when(micro.getLocation()).thenReturn(Location.UNKNOWN);
        Mockito.when(micro.getUuid()).thenReturn(UUID.randomUUID());
        return micro;
    }

    private void markAsWorking(RestApi api) {
        Mockito.when(api.isFaulty()).thenReturn(false);
    }

    private void markAsFaulty(RestApi api) {
        Mockito.when(api.isFaulty()).thenReturn(true);
    }

    private RestApi newRestApi(String name) {
        RestApi api = Mockito.mock(RestApi.class);
        Mockito.when(api.toString()).thenReturn(name);
        Mockito.when(api.getName()).thenReturn(name);
        Mockito.when(api.getHost()).thenReturn("127.0.0.1");
        return api;
    }

    private RestApi newRestApiWithHighPriority(String name) {
        RestApi api = newRestApi(name);
        Mockito.when(api.getPriority()).thenReturn(5);
        return api;
    }

    private RestApi newRestApiWithAffinity(String name) {
        RestApi api = newRestApi(name);
        Mockito.when(api.hasAffinity()).thenReturn(true);
        return api;
    }

    private RestApi newRestApiWithHost(String name, String host) {
        RestApi api = newRestApi(name);
        Mockito.when(api.getHost()).thenReturn(host);
        return api;
    }

    private ApiList apiList() {
        if (apiList == null)
            apiList = new ApiList(routing, locations);

        return apiList;
    }
}
