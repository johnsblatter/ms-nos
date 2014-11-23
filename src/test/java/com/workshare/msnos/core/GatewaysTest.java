package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;

public class GatewaysTest {

    private Agent selfAgent;

    @Before
    public void prepare() {
        selfAgent = mock(Agent.class);
        Iden selfIden = newAgentIden();
        when(selfAgent.getIden()).thenReturn(selfIden);
        
        Gateways.reset();
    }
    

    @Test
    public void shouldGatewaysOfReturnOnlyMine() throws MsnosException {
        Endpoint otherOne = install(new HttpEndpoint(mock(Network.class), "http:/one", newAgentIden()));
        Endpoint otherTwo = install(new HttpEndpoint(mock(Network.class), "http:/two", newAgentIden()));
        Endpoint expected = install(new HttpEndpoint(mock(Network.class), "http:/me", selfAgent.getIden()));

        final Set<Endpoint> allEndpoints = Gateways.allEndpoints();
        int totalEndpoints = allEndpoints.size();
        Set<Endpoint> endpoints = Gateways.endpointsOf(selfAgent);

        assertEquals(totalEndpoints-2, endpoints.size());
        assertTrue(endpoints.contains(expected));
        assertFalse(endpoints.contains(otherOne));
        assertFalse(endpoints.contains(otherTwo));
    }

    @Test
    public void shouldGatewaysOfReturnOnlyPublic() throws MsnosException {
        Set<Endpoint> expected = Gateways.allEndpoints();
        install(new HttpEndpoint(mock(Network.class), "http:/one", newAgentIden()));
        install(new HttpEndpoint(mock(Network.class), "http:/two", newAgentIden()));

        Set<Endpoint> endpoints = Gateways.allPublicEndpoints();

        assertEquals(expected, endpoints);
    }

    @Test
    public void shouldAllGatewaysReturnAll() throws MsnosException {
        
        Endpoint otherOne = install(new HttpEndpoint(mock(Network.class), "http:/one", newAgentIden()));
        Endpoint otherTwo = install(new HttpEndpoint(mock(Network.class), "http:/two", newAgentIden()));
        Endpoint expected = install(new HttpEndpoint(mock(Network.class), "http:/me", selfAgent.getIden()));

        final Set<Endpoint> endpoints = Gateways.allEndpoints();

        assertTrue(endpoints.contains(expected));
        assertTrue(endpoints.contains(otherOne));
        assertTrue(endpoints.contains(otherTwo));
    }

    private Endpoint install(HttpEndpoint endpoint) throws MsnosException {
        for (Gateway gate : Gateways.all()) {
            if (gate instanceof HttpGateway) {
                gate.endpoints().install(endpoint);
            }
        }
        
        return endpoint;
    }

}
