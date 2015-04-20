package com.workshare.msnos.core.receipts;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.protocols.ip.NullGateway;
import com.workshare.msnos.core.Receipt;

public class DelegateReceipt implements Receipt {

    private volatile Receipt delegate = this;

    public DelegateReceipt(Message message) {
        delegate = new SingleReceipt(NullGateway.NAME, Status.UNKNOWN, message);
    }

    public void setDelegate(Receipt delegate) {
        this.delegate = delegate;
    }

    @Override
    public UUID getMessageUuid() {
        return delegate.getMessageUuid();
    }

    @Override
    public Status getStatus() {
        return delegate.getStatus();
    }

    @Override
    public String getGate() {
        return delegate.getGate();
    }

    @Override
    public boolean waitForDelivery(long amount, TimeUnit unit) throws InterruptedException {

        final long tenth = unit.toMillis(amount)/10;
        while(amount > 0) {
            if (isDelegatePresent())
                break;

            justWait(tenth);
            amount-=tenth;
        }

        if (amount > 0)
            return delegate.waitForDelivery(amount, unit);
        else
            return false;
    }

    private boolean isDelegatePresent() {
        return delegate.getGate() != NullGateway.NAME;
    }

    private synchronized void justWait(long millis) throws InterruptedException {
        this.wait(millis);
    }

}
