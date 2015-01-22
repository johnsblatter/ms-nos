package com.workshare.msnos.core;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.workshare.msnos.core.Message.Status;

public class SingleReceipt implements Receipt {

    private final UUID messageUuid;
    private final String gate;
    
    private Status status;

    public SingleReceipt(Gateway gateway, Status status, Message message) {
        this(gateway.name(), status, message);
    }

    private SingleReceipt(String gatewayName, Status status, Message message) {
        this.status = status;
        this.messageUuid = message.getUuid();
        this.gate = gatewayName;
    }

    @Override
    public UUID getMessageUuid() {
        return messageUuid;
    }

    @Override
    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status newStatus) {
        status = newStatus;
        notifyAll();
    }

    @Override
    public synchronized boolean waitForDelivery(long amount, TimeUnit unit) throws InterruptedException {
        switch (status) {
            case DELIVERED:
                return true;

            case FAILED:
            case UNKNOWN:
                return false;

            default:
                final long tt = unit.toMillis(amount);
                this.wait(tt);
                return status == Status.DELIVERED;
        }
    }

    @Override
    public String getGate() {
        return gate;
    }
    
    @Override
    public String toString() {
        return getStatus()+":"+messageUuid;
    }
    
    public static SingleReceipt failure(Message message) {
        return new SingleReceipt("none", Status.FAILED, message);
    }
}
