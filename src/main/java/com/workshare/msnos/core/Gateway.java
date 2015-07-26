package com.workshare.msnos.core;

import java.io.IOException;

import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.core.protocols.ip.Endpoints;

public interface Gateway {

    public interface Listener {
        public void onMessage(Message message);
    }

    public String name();

    public void addListener(Cloud cloud, Listener listener);

    public Endpoints endpoints();

    public Receipt send(Cloud cloud, Message message, Identifiable to) throws IOException;

    public void close() throws IOException;
}