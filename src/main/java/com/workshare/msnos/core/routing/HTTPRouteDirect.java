package com.workshare.msnos.core.routing;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;

public class HTTPRouteDirect extends Route {

    public HTTPRouteDirect(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) {
        RemoteAgent remote = cloud.getRemoteAgent(message.getTo());
        if (remote != null) {
            if (router.hasRouteFor(remote)) {
                final Receipt receipt = router.sendViaHTTP(message, remote, 0, "HTTP-DIRECT");
                if (receipt.getStatus() == Status.DELIVERED) {
                    return receipt;
                }
            }
        }

        return null;
    }
}
