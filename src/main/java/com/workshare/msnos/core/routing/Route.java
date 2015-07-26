package com.workshare.msnos.core.routing;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.cloud.Cloud;

abstract class Route {
    protected static final Logger routing = LoggerFactory.getLogger("routing");
    
    protected final Router router;
    protected final Cloud cloud;

    public Route(Router router) {
        this.router = router;
        this.cloud = router.cloud();
    }
    
    /**
     * @return null if route is not applicable or a valid receipt
     * @throws IOException 
     */
    public abstract Receipt send(Message message);
}