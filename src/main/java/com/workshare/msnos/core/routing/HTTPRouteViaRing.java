package com.workshare.msnos.core.routing;

import java.io.IOException;
import java.util.Collection;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Ring;

public class HTTPRouteViaRing extends Route {

    public HTTPRouteViaRing(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) throws IOException {
        RemoteAgent remote = cloud.getRemoteAgent(message.getTo());
        if (remote == null)
            return null;

        Ring ring = remote.getRing();
        Collection<RemoteAgent> agents = cloud.getRemoteAgents();
        for (RemoteAgent agent : agents) {
            if (ring.equals(agent.getRing())) {
                if (router.hasRoute(agent)) {
                    return router.sendViaHTTP(message, 1, "HTTP-VIA-RING");
                }
            }
        }

        return null;
    }
}
