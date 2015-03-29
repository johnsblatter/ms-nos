package com.workshare.msnos.usvc;

import static com.workshare.msnos.core.CoreHelper.fakeSystemTime;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;

public class RemoteMicroserviceTest {

    private Ring ring;

    @Before
    public void before() {
        ring = mock(Ring.class);
        Location location = mock(Location.class);
        when(ring.location()).thenReturn(location );
    }

    @After
    public void afterTest() {
        SystemTime.reset();
    }
    
    @Test
    public void shouldFixRestApisWhenHostIsMissing() {
        RemoteMicroservice micro = createRemoteMicroservice();
        
        Set<RestApi> apis = micro.getApis();
        assertEquals("25.25.25.25", first(apis).getHost());
    }

    @Test
    public void shouldUpdatedLastUpdatedWehnApisSet() {
        fakeSystemTime(10000);
        RemoteMicroservice micro = createRemoteMicroservice();
        assertEquals(10000, micro.getLastUpdated());

        fakeSystemTime(99999);
        micro.setApis(asSet(new RestApi("path", 2222)));
        assertEquals(99999, micro.getLastUpdated());
    }
    
    @Test
    public void shouldUpdatedLastCheckTimeWehnMarkedWorkingOrFaulty() {
        RemoteMicroservice micro = createRemoteMicroservice();

        fakeSystemTime(11111);
        micro.markFaulty();
        assertEquals(11111, micro.getLastChecked());

        fakeSystemTime(22222);
        micro.markWorking();
        assertEquals(22222, micro.getLastChecked());
    }
    
    @Test
    public void shouldNotifyRing() {
        RemoteMicroservice micro = createRemoteMicroservice();
        verify(ring).onMicroserviceJoin(micro);
    }

    @Test
    public void shouldUseRingLocationIfAvailable() {
        RemoteMicroservice micro = createRemoteMicroservice();
        assertEquals(ring.location(), micro.getLocation());
    }


    private <T> Set<T> asSet(T... elements) {
        return new HashSet<T>(Arrays.asList(elements));
    }

    private <T> T first(Collection<T> elements) {
        return elements.iterator().next();
    }

    private RemoteMicroservice createRemoteMicroservice() {
        RemoteAgent agent = mock(RemoteAgent.class);
        final Endpoint endpoint = new BaseEndpoint(Type.UDP, new Network(new byte[]{25,25,25,25}, (short)15));
        when(agent.getEndpoints()).thenReturn(asSet(endpoint));
        when(agent.getRing()).thenReturn(ring);

        RemoteMicroservice micro = new RemoteMicroservice("foo" , agent, asSet(new RestApi("path", 1234)));
        return micro;
    }
}
