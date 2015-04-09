package com.workshare.msnos.core.routing;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.SingleReceipt;

public class FailingRouteOnZeroHops extends Route {
    public FailingRouteOnZeroHops(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) {
        if (message.getHops() == 0) {
            routing.debug("XX === FAIL-ZERO_HOPS === {}", message);
            return SingleReceipt.failure(message);
        }
        else
            return null;
    }
}