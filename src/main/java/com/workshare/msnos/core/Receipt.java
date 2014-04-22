package com.workshare.msnos.core;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.workshare.msnos.core.Message.Status;

public interface Receipt {

    public abstract UUID getMessageUuid();

    public abstract Status getStatus();

    public abstract boolean waitForDelivery(long amount, TimeUnit unit) throws InterruptedException;

}