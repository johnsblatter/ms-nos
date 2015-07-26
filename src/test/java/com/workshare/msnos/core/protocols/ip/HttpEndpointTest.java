package com.workshare.msnos.core.protocols.ip;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.workshare.msnos.core.cloud.CoreHelper.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.junit.Test;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.services.api.RemoteMicroservice;
import com.workshare.msnos.core.services.api.RestApi;

public class HttpEndpointTest {

    @Test
    public void shouldAddNetworkFromRemoteService() {
        RemoteMicroservice remote = createRemoteMicroservice("25.25.25.25");
        RestApi api = new RestApi("path/to/api", 8888, "25.25.25.25");
    
        HttpEndpoint endpoint = new HttpEndpoint(remote, api);
        
        assertEquals(8888, endpoint.getPort());
        assertEquals(api.getUrl(), endpoint.getUrl());
        assertEquals(remote.getAgent().getIden(), endpoint.getTarget());
        assertEquals(asPublicNetwork("25.25.25.25"), endpoint.getNetwork());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfNetworkNotFound() {
        RemoteMicroservice remote = createRemoteMicroservice("25.25.25.25");
        RestApi api = new RestApi("path/to/api", 8888, "99.99.99.99");
    
        new HttpEndpoint(remote, api);
    }
    
    @Test
    public void shouldSupportEqualdAndHashcode() {
        RemoteMicroservice remote = createRemoteMicroservice("25.25.25.25");
        RestApi api = new RestApi("path/to/api", 8888, "25.25.25.25");
    
        HttpEndpoint ep1 = new HttpEndpoint(remote, api);
        HttpEndpoint ep2 = new HttpEndpoint(remote, api);
        
        assertEquals(ep1, ep2);
        assertEquals(ep1.hashCode(), ep2.hashCode());
    }
    
    protected RemoteMicroservice createRemoteMicroservice(final String host) {
        BaseEndpoint ep1 = new BaseEndpoint(Type.UDP, asPublicNetwork(host));
        BaseEndpoint ep2 = new BaseEndpoint(Type.UDP, asPublicNetwork("33.33.33.33"));
        BaseEndpoint ep3 = new BaseEndpoint(Type.UDP, asPublicNetwork("11.11.11.11"));
        
        RemoteAgent agent = mock(RemoteAgent.class);
        when(agent.getIden()).thenReturn(new Iden(Iden.Type.AGT, UUID.randomUUID()));
        when(agent.getEndpoints()).thenReturn(new HashSet<Endpoint>(Arrays.asList(ep1, ep2, ep3)));
        
        RemoteMicroservice remote = mock(RemoteMicroservice.class);
        when(remote.getAgent()).thenReturn(agent);

        return remote;
    }
}
