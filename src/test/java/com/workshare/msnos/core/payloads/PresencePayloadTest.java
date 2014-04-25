package com.workshare.msnos.core.payloads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.protocols.ip.Network;

public class PresencePayloadTest {

    @Test
    public void shouldSplitNetworksCollectionOnSplit() {
        Network alfa = Mockito.mock(Network.class);
        Network beta = Mockito.mock(Network.class);
        Set<Network> networks = new HashSet<Network>(Arrays.asList(alfa, beta));
        Presence payload = new Presence(true, networks);

        Payload[] loads = payload.split();
        assertEquals(2, loads.length);

        Set<Network> newNets = new HashSet<Network>();
        newNets.add(getNet(loads[0]));
        newNets.add(getNet(loads[1]));
        assertEquals(payload.getNetworks(), newNets);
    }

    @Test
    public void shouldKeepPresenceFlahOnSplit() {
        Network alfa = Mockito.mock(Network.class);
        Network beta = Mockito.mock(Network.class);
        Set<Network> networks = new HashSet<Network>(Arrays.asList(alfa, beta));
        Presence payload = new Presence(true, networks);

        Payload[] loads = payload.split();
        for (Payload load : loads) {
            assertTrue(((Presence) load).isPresent());
        }
    }

    private Network getNet(Payload payload) {
        Presence load = (Presence) payload;
        Set<Network> nets = load.getNetworks();
        if (nets.size() == 1)
            return nets.iterator().next();
        else
            throw new AssertionError("One network only was expected!");
    }
}
