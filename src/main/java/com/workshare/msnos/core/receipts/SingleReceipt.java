package com.workshare.msnos.core.receipts;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.protocols.ip.NullGateway;
import com.workshare.msnos.core.Receipt;

public class SingleReceipt implements Receipt {

    private final UUID messageUuid;
    
    private String gate;
    private Status status;

    public SingleReceipt(Gateway gateway, Status status, Message message) {
        this(gateway.name(), status, message);
    }

    protected SingleReceipt(String gatewayName, Status status, Message message) {
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

    public synchronized void update(Receipt other) {
        if (!messageUuid.equals(other.getMessageUuid()))
            throw new IllegalArgumentException("You cannot update a receipt related to another message!");

        this.gate = other.getGate();
        this.status = other.getStatus();
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
        return new SingleReceipt(NullGateway.NAME, Status.FAILED, message);
    }
    
    public static SingleReceipt unknown(Message message) {
        return new SingleReceipt(NullGateway.NAME, Status.UNKNOWN, message);
    }
}
