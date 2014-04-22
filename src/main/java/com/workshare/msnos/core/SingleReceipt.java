package com.workshare.msnos.core;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.workshare.msnos.core.Message.Status;

public class SingleReceipt implements Receipt {

    private Status status;
    private final UUID messageUuid;

    public SingleReceipt(Status status, Message message) {
        this.status = status;
        this.messageUuid = message.getUuid();
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

            case UNKNOWN:
                return false;

            default:
                final long tt = unit.toMillis(amount);
                this.wait(tt);
                return status == Status.DELIVERED;
        }
    }
}
