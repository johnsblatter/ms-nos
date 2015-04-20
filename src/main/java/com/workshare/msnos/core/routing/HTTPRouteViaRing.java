package com.workshare.msnos.core.routing;

import java.util.Collection;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Ring;

public class HTTPRouteViaRing extends Route {

    public HTTPRouteViaRing(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message)  {
        RemoteAgent remote = cloud.getRemoteAgent(message.getTo());
        if (remote == null)
            return null;

        Ring ring = remote.getRing();
        Collection<RemoteAgent> agents = cloud.getRemoteAgents();
        for (RemoteAgent agent : agents) {
            if (agent.getIden().equals(message.getTo()))
                continue;
            
            if (!ring.equals(agent.getRing()))
                continue;

            if (!router.hasRouteFor(agent))
                continue;

            final Receipt receipt = router.sendViaHTTP(message, agent, 1, "HTTP-RINGD");
            if (receipt.getStatus() == Status.DELIVERED) {
                return receipt;
            }
        }

        return null;
    }
}
