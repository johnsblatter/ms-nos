package com.workshare.msnos.core.payloads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Network;

public class PresencePayloadTest {

    @Test
    public void shouldSplitNetworksCollectionOnSplit() {
        Endpoint alfa = Mockito.mock(Endpoint.class);
        Endpoint beta = Mockito.mock(Endpoint.class);
        Set<Endpoint> endpoints = new HashSet<Endpoint>(Arrays.asList(alfa, beta));
        Presence payload = new Presence(true, endpoints);

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
        Set<Endpoint> endpoints = new HashSet<Endpoint>(Arrays.asList(alfa, beta));
        Presence payload = new Presence(true, endpoints);

        Payload[] loads = payload.split();
        for (Payload load : loads) {
            assertTrue(((Presence) load).isPresent());
        }
    }

    private Endpoint getEndpoint(Payload payload) {
        Presence load = (Presence) payload;
        Set<Endpoint> nets = load.getEndpoints();
        if (nets.size() == 1)
            return nets.iterator().next();
        else
            throw new AssertionError("One network only was expected!");
    }
}
