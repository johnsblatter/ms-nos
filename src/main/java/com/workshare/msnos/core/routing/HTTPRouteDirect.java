package com.workshare.msnos.core.routing;

import java.io.IOException;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;

public class HTTPRouteDirect extends Route {

    public HTTPRouteDirect(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) throws IOException {
        RemoteAgent remote = cloud.getRemoteAgent(message.getTo());
        if (remote != null) {
            if (router.hasRoute(remote)) {
                return router.sendViaHTTP(message, 0, "DIRECT-HTTP");
            }
        }

        return null;
    }
}
