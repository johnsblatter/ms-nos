package com.workshare.msnos.core.routing;

import java.io.IOException;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;

public class WWWRouteBroadcast extends Route {

    public WWWRouteBroadcast(Router router) {
        super(router);
    }

    @Override
    public Receipt send(Message message) throws IOException {
        router.sendViaWWW(message, "WWW-BROADCAST");
        return null;
    }
}
