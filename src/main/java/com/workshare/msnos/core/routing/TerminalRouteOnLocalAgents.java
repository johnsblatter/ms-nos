package com.workshare.msnos.core.routing;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;

public class TerminalRouteOnLocalAgents extends Route {
    
    public TerminalRouteOnLocalAgents(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) {
        if (cloud.getLocalAgent(message.getTo()) != null) {
            return router.terminal(message, "TO-LOCAL");
        }
        else
            return null;
    }
}