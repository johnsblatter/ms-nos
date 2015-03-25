package com.workshare.msnos.core.routing;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.SingleReceipt;

public class FailingRouteZeroHops extends Route {
    public FailingRouteZeroHops(Router router) {
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