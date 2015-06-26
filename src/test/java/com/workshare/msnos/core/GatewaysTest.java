package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.www.WWWGateway;

@SuppressWarnings("restriction")
public class GatewaysTest {

    private Agent selfAgent;
    private HttpServer httpServer;

    @Before
    public void prepare() {
        selfAgent = mock(Agent.class);
        Iden selfIden = newAgentIden();
        when(selfAgent.getIden()).thenReturn(selfIden);
        
        Gateways.reset();
    }
    
    @After
    public void cleanup() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        
        System.getProperties().remove(Gateways.SYSP_GATE_HTTP_DISABLE);
        System.getProperties().remove(Gateways.SYSP_GATE_UDP_DISABLE);
        System.getProperties().remove(Gateways.SYSP_GATE_WWW_DISABLE);
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
    public void shouldAllPublicEndpointsReturnOnlyPublic() throws MsnosException {
        Set<Endpoint> expected = Gateways.allEndpoints();
        install(new HttpEndpoint(mock(Network.class), "http:/one", newAgentIden()));
        install(new HttpEndpoint(mock(Network.class), "http:/two", newAgentIden()));

        Set<Endpoint> endpoints = Gateways.allPublicEndpoints();

        assertEquals(expected, endpoints);
    }

    @Test
    public void shouldAllEndpointsReturnAll() throws MsnosException {
        
        Endpoint otherOne = install(new HttpEndpoint(mock(Network.class), "http:/one", newAgentIden()));
        Endpoint otherTwo = install(new HttpEndpoint(mock(Network.class), "http:/two", newAgentIden()));
        Endpoint expected = install(new HttpEndpoint(mock(Network.class), "http:/me", selfAgent.getIden()));

        final Set<Endpoint> endpoints = Gateways.allEndpoints();

        assertTrue(endpoints.contains(expected));
        assertTrue(endpoints.contains(otherOne));
        assertTrue(endpoints.contains(otherTwo));
    }

    @Test
    public void shouldAllGatewaysNotReturnHttpIfDisabled() throws Exception {
        setupFakeWWWGateway();
        System.setProperty(Gateways.SYSP_GATE_HTTP_DISABLE, "true");
        
        Set<Gateway> gates = Gateways.all();
        
        assertGateMissing(gates, HttpGateway.class);
        assertGatePresent(gates, UDPGateway.class);
        assertGatePresent(gates, WWWGateway.class);
    }

    @Test
    public void shouldAllGatewaysNotReturnUDPIfDisabled() throws Exception {
        setupFakeWWWGateway();
        System.setProperty(Gateways.SYSP_GATE_UDP_DISABLE, "true");
        
        Set<Gateway> gates = Gateways.all();
        
        assertGatePresent(gates, HttpGateway.class);
        assertGateMissing(gates, UDPGateway.class);
        assertGatePresent(gates, WWWGateway.class);
    }

    @Test
    public void shouldAllGatewaysNotReturnWWWIfDisabled() throws Exception {
        setupFakeWWWGateway();
        System.setProperty(Gateways.SYSP_GATE_WWW_DISABLE, "true");
        
        Set<Gateway> gates = Gateways.all();
        
        assertGatePresent(gates, HttpGateway.class);
        assertGatePresent(gates, UDPGateway.class);
        assertGateMissing(gates, WWWGateway.class);
    }


    private void setupFakeWWWGateway() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(selectRandomTCPPOrt()), 0);
        httpServer.setExecutor(null);
        httpServer.createContext("/", new HttpHandler(){
            @Override
            public void handle(HttpExchange exch) throws IOException {
                exch.sendResponseHeaders(200, 0l);
                exch.getResponseBody().close();
            }});
        httpServer.start();

        InetSocketAddress address = httpServer.getAddress();
        int port = address.getPort();
        System.setProperty(WWWGateway.SYSP_ADDRESS, "http://127.0.0.1:"+port);
    }


    public void assertGateMissing(Set<Gateway> gates, final Class<?> gateClass) {
        for (Gateway gate : gates) {
            assertFalse("Gateway "+gateClass.getSimpleName()+" unexpectedly found", gate.getClass().equals(gateClass));
        }
    }
    public void assertGatePresent(Set<Gateway> gates, final Class<?> gateClass) {
        for (Gateway gate : gates) {
           if (gate.getClass().equals(gateClass))
               return;
        }
        
        fail("Gateway "+gateClass.getSimpleName()+" not found!");
    }



    private Endpoint install(HttpEndpoint endpoint) throws MsnosException {
        for (Gateway gate : Gateways.all()) {
            if (gate instanceof HttpGateway) {
                gate.endpoints().install(endpoint);
            }
        }
        
        return endpoint;
    }

    private int selectRandomTCPPOrt() throws IOException {
        int port;
        ServerSocket s = new ServerSocket(0);
        try {
            port = s.getLocalPort();
        }
        finally {
            s.close();
        }
        return port;
    }
}
