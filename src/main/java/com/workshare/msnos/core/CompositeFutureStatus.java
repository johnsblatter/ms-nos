package com.workshare.msnos.core;

import java.util.concurrent.Future;

interface CompositeFutureStatus extends Future<Message.Status> {
    public void add( Future<Message.Status> status)
    ;
}
