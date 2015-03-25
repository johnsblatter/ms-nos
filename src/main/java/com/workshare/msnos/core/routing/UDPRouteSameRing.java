package com.workshare.msnos.core.routing;

import java.io.IOException;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;

public class UDPRouteSameRing extends Route {

    public UDPRouteSameRing(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) throws IOException {
        RemoteAgent remote = cloud.getRemoteAgent(message.getTo());
        if (remote != null && remote.getRing().equals(cloud.getRing())) {
            return router.sendViaUDP(message, 0, "RING-UDP");
        }

        return null;
    }
}
