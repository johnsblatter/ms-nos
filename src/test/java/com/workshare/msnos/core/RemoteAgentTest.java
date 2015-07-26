package com.workshare.msnos.core;

import static com.workshare.msnos.core.cloud.CoreHelper.asPublicNetwork;
import static com.workshare.msnos.core.cloud.CoreHelper.asSet;
import static com.workshare.msnos.core.cloud.CoreHelper.fakeSystemTime;
import static com.workshare.msnos.core.cloud.CoreHelper.newAgentIden;
import static com.workshare.msnos.core.cloud.CoreHelper.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.time.SystemTime;

public class RemoteAgentTest {

    public static final Network NETWORK = asPublicNetwork("25.25.25.25");

    private Cloud cloud;

    @Before
    public void before() throws Exception {
        cloud = mock(Cloud.class);
    }

    @After
    public void after() throws Exception {
        SystemTime.reset();
    }

    @Test
    public void shouldTouchWhenCreated() {
        fakeSystemTime(12345L);
        
        Set<Endpoint> endpoints = Collections.emptySet();
        RemoteAgent agent = new RemoteAgent(randomUUID(), cloud, endpoints);
        
        assertEquals(12345L, agent.getAccessTime());
    }

    @Test
    public void shouldNotReturnHttpEndpointsIfNotPresent() {
        Set<Endpoint> endpoints = Collections.emptySet();
        RemoteAgent agent = new RemoteAgent(randomUUID(), cloud, endpoints);
        
        assertEquals(0, agent.getEndpoints(Endpoint.Type.HTTP).size());
    }

    @Test
    public void shouldNotReturnUDPEndpointsIfNotPresent() {
        Set<Endpoint> endpoints = Collections.emptySet();
        RemoteAgent agent = new RemoteAgent(randomUUID(), cloud, endpoints);
        
        assertEquals(0, agent.getEndpoints(Endpoint.Type.UDP).size());
    }

    @Test
    public void shouldReturnHttpEndpointsIfPresent() {
        Iden iden = newAgentIden();
        Endpoint http = newHttpEndpoint(iden);
        RemoteAgent agent = new RemoteAgent(iden.getUUID(), cloud, asSet(http));
        
        final Set<Endpoint> points = agent.getEndpoints(Endpoint.Type.HTTP);

        assertEquals(1, points.size());
        assertEquals(http, first(points));
    }

    @Test
    public void shouldReturnUDPEndpointsIfPresent() {
        Endpoint udp = new BaseEndpoint(Endpoint.Type.UDP, NETWORK);
        RemoteAgent agent = new RemoteAgent(newAgentIden().getUUID(), cloud, asSet(udp));
        
        final Set<Endpoint> points = agent.getEndpoints(Endpoint.Type.UDP);

        assertEquals(1, points.size());
        assertEquals(udp, first(points));
    }

    public static <T> T first(Set<T> set) {
        return set.iterator().next();
    }

    public static  HttpEndpoint newHttpEndpoint(Iden iden) {
        return new HttpEndpoint(NETWORK, "http://foo", iden );
    }
}
