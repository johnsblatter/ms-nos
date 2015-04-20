package com.workshare.msnos.core.routing;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;

public class WWWRouteBroadcast extends Route {

    public WWWRouteBroadcast(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) {
        router.sendViaWWW(message, "WWW-BROADCAST");
        return null;
    }
}
