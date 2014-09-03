package com.workshare.msnos.usvc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.usvc.api.RestApi;

public class RemoteMicroserviceTest {
    
    @Test
    public void shouldFixRestApisWhenHostIsMissing() {
        RemoteAgent agent = mock(RemoteAgent.class);
        final Endpoint endpoint = new Endpoint(Type.UDP, new Network(new byte[]{25,25,25,25}, (short)15));
        when(agent.getEndpoints()).thenReturn(asSet(endpoint));

        RemoteMicroservice micro = new RemoteMicroservice("foo" , agent, asSet(new RestApi("api", "path", 1234)));
        
        Set<RestApi> apis = micro.getApis();
        assertEquals("25.25.25.25", first(apis).getHost());
    }

    private <T> Set<T> asSet(T... elements) {
        return new HashSet<T>(Arrays.asList(elements));
    }

    private <T> T first(Collection<T> elements) {
        return elements.iterator().next();
    }

}
