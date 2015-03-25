package com.workshare.msnos.core.routing;

import java.io.IOException;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;

public class UDPRouteBroadcast extends Route {

    private final int maximumHops;

    public UDPRouteBroadcast(Router router) {
        super(router);
        maximumHops = Integer.getInteger(Router.SYSP_MAXIMUM_HOPS, 10);
    }

    @Override
    public Receipt send(Message message) throws IOException {
        return router.sendViaUDP(message, maximumHops, "UDP-BROADCAST");
    }
}
