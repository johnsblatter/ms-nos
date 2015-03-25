package com.workshare.msnos.core.routing;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.SingleReceipt;

public class FailingRouteMessageSeen extends Route {

    public FailingRouteMessageSeen(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) {
        if (router.wasSeen(message)) {
            routing.debug("XX === WAS_SEEN === {}", message);
            return SingleReceipt.failure(message);
        }
        else
            return null;
    }

}
