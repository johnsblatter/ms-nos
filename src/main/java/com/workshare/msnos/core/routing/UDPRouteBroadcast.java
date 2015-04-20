package com.workshare.msnos.core.routing;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;

public class UDPRouteBroadcast extends Route {

    private final int maximumHops;

    public UDPRouteBroadcast(Router router) {
        super(router);
        maximumHops = Integer.getInteger(Router.SYSP_MAXIMUM_HOPS_DIRECT, 10);
    }

    @Override
    public Receipt send(Message message)  {
        return router.sendViaUDP(message, maximumHops, "UDP-BROADCAST");
    }
}
