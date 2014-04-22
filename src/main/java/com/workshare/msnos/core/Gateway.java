package com.workshare.msnos.core;

import java.io.IOException;
import java.util.Set;

import com.workshare.msnos.core.protocols.ip.Endpoint;

public interface Gateway {

    public interface Listener {
        public void onMessage(Message message)
        ;
    }

    public void addListener(Listener listener)
    ;

    public Set<? extends Endpoint> endpoints()
    ;

    public Receipt send(Message message) throws IOException
    ;
}