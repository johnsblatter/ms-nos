package com.workshare.msnos.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.workshare.msnos.core.Message.Status;

public class MultiReceipt implements Receipt {

    private final UUID messageUuid;
    private final Set<Receipt> receipts;
     
    public MultiReceipt(Message message, Receipt... someReceipts) {
        this(message, Arrays.asList(someReceipts));
    }
    
    public MultiReceipt(Message message, List<Receipt> someReceipts) {
        this.messageUuid = message.getUuid();
        this.receipts = Collections.newSetFromMap(new ConcurrentHashMap<Receipt, Boolean>());
        receipts.addAll(someReceipts);
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
            if (receipt.getStatus() == Status.FAILED  && status == Status.UNKNOWN) {
                status = Status.FAILED;
            }
            else if (receipt.getStatus() == Status.PENDING) {
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

        final long tenth = unit.toMillis(amount)/10;
        while(amount > 0) {
            amount-=tenth;
            if (receipts.size() == 0)
                justWait(tenth);
            else
                if (waitForDelivery(tenth))
                    return true;
        }

        return getStatus() == Status.DELIVERED;
    }

    private synchronized void justWait(long millis) throws InterruptedException {
        this.wait(millis);
    }

    private boolean waitForDelivery(long millis) throws InterruptedException {
        long slice = millis / receipts.size();
        for (Receipt receipt : receipts) {
            boolean res = receipt.waitForDelivery(slice, TimeUnit.MILLISECONDS);
            if (res == true)
                return true;
        }
        
        return false;
    }

    public int size() {
        return receipts.size();
    }

    @Override
    public String getGate() {
        StringBuilder buffer = new StringBuilder();
        for (Receipt receipt : receipts) {
            if (receipt.getStatus() == Status.DELIVERED)
                return receipt.getGate();

            if (receipt.getStatus() == Status.FAILED)
                continue;

            if (buffer.length() > 0)
                buffer.append("+");
            buffer.append(receipt.getGate());
        }
        
        return buffer.toString();
    }

    @Override
    public String toString() {
        return getStatus()+":"+messageUuid;
    }
}
