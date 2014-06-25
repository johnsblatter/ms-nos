package com.workshare.msnos.core.cloud;

import java.util.concurrent.Executor;

import com.workshare.msnos.core.Cloud.Listener;
import com.workshare.msnos.core.Message;

public class Multicaster extends com.workshare.msnos.soup.threading.Multicaster<Listener, Message> {
    public Multicaster() {
        super();
    }

    public Multicaster(Executor executor) {
        super(executor);
    }

    @Override
    protected void dispatch(Listener listener, Message message) {
        listener.onMessage(message);
    }
}