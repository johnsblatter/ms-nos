package com.workshare.msnos.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.workshare.msnos.core.Message.Status;

public class MultiGatewayReceipt implements Receipt {

    private final Set<Receipt> receipts;
    private final UUID messageUuid;

    public MultiGatewayReceipt(Message message, Receipt... someReceipts) {
        this.messageUuid = message.getUuid();
        this.receipts = Collections.newSetFromMap(new ConcurrentHashMap<Receipt, Boolean>());
        receipts.addAll(Arrays.asList(someReceipts));
    }
    
    public void add(Receipt receipt) {
        receipts.add(receipt);
    }

    public Set<Receipt> getReceipts() {
        return Collections.unmodifiableSet(receipts);
    }

    public UUID getMessageUuid() {
        return messageUuid;
    }

    public Status getStatus() {
        Status status = Status.UNKNOWN;
        for (Receipt receipt : receipts) {
            if (receipt.getStatus() == Status.PENDING) {
                status = Status.PENDING;
            }
            else if (receipt.getStatus() == Status.DELIVERED) {
                status = Status.DELIVERED;
                break;
            }
        }

        return status;
    }

    public boolean waitForDelivery(long amount, TimeUnit unit) throws InterruptedException {
        long slice = unit.toMillis(amount) / receipts.size();
        for (Receipt receipt : receipts) {
            boolean res = receipt.waitForDelivery(slice, TimeUnit.MILLISECONDS);
            if (res == true)
                return true;
        }

        return getStatus() == Status.DELIVERED;
    }

    public int size() {
        return receipts.size();
    }

}
