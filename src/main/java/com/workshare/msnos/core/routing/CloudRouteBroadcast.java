package com.workshare.msnos.core.routing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MultiReceipt;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Iden.Type;
import com.workshare.msnos.core.Ring;

public class CloudRouteBroadcast extends Route {

    private final int maximumHops;
    private final int maximumMessagesForRing;

    public CloudRouteBroadcast(Router router) {
        super(router);
        maximumHops = Integer.getInteger(Router.SYSP_MAXIMUM_HOPS_CLOUD, 10);
        maximumMessagesForRing = Integer.getInteger(Router.SYSP_MAXIMUM_MESSAGES_PER_RING, 2);
    }

    @Override
    public Receipt send(Message message) throws IOException {
        if (message.getTo().getType() != Type.CLD)
            return null;

        List<Receipt> receipts = new ArrayList<Receipt>();
        sendViaUDP(message, receipts);
        sendViaHTTP(message, receipts);

        return new MultiReceipt(message, receipts);
    }

    private void sendViaHTTP(Message message, List<Receipt> receipts) throws IOException {

        RingCounter rings = new RingCounter();
        
        Collection<RemoteAgent> agents = cloud.getRemoteAgents();
        for (RemoteAgent remote : agents) {
            final Ring ring = remote.getRing();
            if (ring.equals(cloud.getRing()))
                continue;

            if (!router.hasRouteFor(remote))
                continue;

            if (rings.size(ring) == maximumMessagesForRing)
                continue;

            receipts.add(router.sendViaHTTP(message, remote, maximumHops, "HTTP-VIA-RING"));
            rings.count(ring);
        }
    }

    private void sendViaUDP(Message message, List<Receipt> receipts) throws IOException {
        receipts.add(router.sendViaUDP(message, maximumHops, "UDP-BROADCAST"));
    }
    
    public static class RingCounter {
        private Map<Ring, Integer> counters = new HashMap<Ring, Integer>();
 
        public void count(Ring ring) {
            Integer val = counters.get(ring);
            if (val == null) 
                val = Integer.valueOf(1);
            else
                val = Integer.valueOf(val.intValue()+1);

            counters.put(ring, val);
        }
        
        public int size(Ring ring) {
            Integer val = counters.get(ring);
            return (val == null ? 0 : val.intValue());
        }
    }
}
