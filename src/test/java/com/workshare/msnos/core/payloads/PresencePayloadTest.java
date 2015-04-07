package com.workshare.msnos.core.payloads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Gateways;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.protocols.ip.Endpoint;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Gateways.class)
public class PresencePayloadTest {

    @Test
    public void shouldSplitEndpointsSetOnSplit() {
        Endpoint alfa = Mockito.mock(Endpoint.class);
        Endpoint beta = Mockito.mock(Endpoint.class);
        Presence payload = new Presence(true, asSet(alfa, beta));

        Payload[] loads = payload.split();
        assertEquals(2, loads.length);

        Set<Endpoint> newdpoints = new HashSet<Endpoint>();
        newdpoints.add(getEndpoint(loads[0]));
        newdpoints.add(getEndpoint(loads[1]));
        assertEquals(payload.getEndpoints(), newdpoints);
    }

    @Test
    public void shouldKeepPresenceFlagOnSplit() {
        Endpoint alfa = Mockito.mock(Endpoint.class);
        Endpoint beta = Mockito.mock(Endpoint.class);
        Presence payload = new Presence(true, asSet(alfa, beta));

        Payload[] loads = payload.split();
        for (Payload load : loads) {
            assertTrue(((Presence) load).isPresent());
        }
    }    

    @Test
    public void shouldLoadMyGatewaysWhenPresenceTrue() throws MsnosException {
        PowerMockito.mockStatic(Gateways.class);

        Endpoint alfa = Mockito.mock(Endpoint.class);
        Endpoint beta = Mockito.mock(Endpoint.class);
        HashSet<Endpoint> expected = asSet(alfa, beta);
        Agent agent = mock(Agent.class);
        when(agent.getEndpoints()).thenReturn(expected);
        
        Presence payload = new Presence(true, agent);

        final Set<Endpoint> current = payload.getEndpoints();
        assertEquals(expected, current);
    }
    
    // FIXME now working on agents endpoints, not on Gateway's'/
    @Test
    public void shouldLoadNoGatewaysWhenPresenceFalse() throws MsnosException {
        PowerMockito.mockStatic(Gateways.class);

        Endpoint alfa = Mockito.mock(Endpoint.class);
        Endpoint beta = Mockito.mock(Endpoint.class);
        HashSet<Endpoint> expected = asSet(alfa, beta);
        when(Gateways.endpointsOf(any(Agent.class))).thenReturn(expected);

        Agent agent = mock(Agent.class);
        Presence payload = new Presence(false, agent);

        assertEquals(0, payload.getEndpoints().size());
    }
    
    private Endpoint getEndpoint(Payload payload) {
        Presence load = (Presence) payload;
        Set<Endpoint> nets = load.getEndpoints();
        if (nets.size() == 1)
            return nets.iterator().next();
        else
            throw new AssertionError("One network only was expected!");
    }

    private HashSet<Endpoint> asSet(Endpoint... endpoints) {
        return new HashSet<Endpoint>(Arrays.asList(endpoints));
    }
}
