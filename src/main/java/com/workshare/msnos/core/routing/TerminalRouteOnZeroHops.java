package com.workshare.msnos.core.routing;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;

public class TerminalRouteOnZeroHops extends Route {
    
    public TerminalRouteOnZeroHops(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) {
        if (message.getHops() == 0) {
            return router.skip(message, "ZERO-HOPS");
        }
        else
            return null;
    }
}