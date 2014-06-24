package com.workshare.msnos.core;

import static org.junit.Assert.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.workshare.msnos.core.Message.Status;

public class ReceiptTest {

    private static final Message MESSAGE = new MessageBuilder(Message.Type.PIN, new LocalAgent(UUID.randomUUID()), new LocalAgent(UUID.randomUUID())).make();

    @Test
    public void shouldStoreMessageUuid() throws Exception {
        Receipt receipt = new SingleReceipt(Status.UNKNOWN, MESSAGE);
        assertEquals(MESSAGE.getUuid(), receipt.getMessageUuid());
    }

    @Test
    public void shouldAwaitReturnFalseForUnknownStatus() throws Exception {
        Receipt receipt = new SingleReceipt(Status.UNKNOWN, MESSAGE);
        assertFalse(receipt.waitForDelivery(1, TimeUnit.SECONDS));
    }

    @Test
    public void shouldAwaitReturnImmediatelyForUnknownStatus() throws Exception {
        Receipt receipt = new SingleReceipt(Status.UNKNOWN, MESSAGE);
        long now = System.currentTimeMillis();
        receipt.waitForDelivery(1, TimeUnit.SECONDS);
        assertEquals(now/10, System.currentTimeMillis()/10, 0.1);
    }
    
    @Test
    public void shouldAwaitReturnTrueForDeliveredStatus() throws Exception {
        Receipt receipt = new SingleReceipt(Status.DELIVERED, MESSAGE);
        assertTrue(receipt.waitForDelivery(1, TimeUnit.SECONDS));
    }

    @Test
    public void shouldAwaitReturnImmediatelyForDeliveredStatus() throws Exception {
        Receipt receipt = new SingleReceipt(Status.DELIVERED, MESSAGE);
        long now = System.currentTimeMillis();
        receipt.waitForDelivery(1, TimeUnit.SECONDS);
        assertEquals(now/10, System.currentTimeMillis()/10, 0.1);
    }

    @Test
    public void shouldAwaitReturnFalseForDeliveredStatusStillPending() throws Exception {
        Receipt receipt = new SingleReceipt(Status.PENDING, MESSAGE);
        assertFalse(receipt.waitForDelivery(10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldAwaitReturnAfterTimeoutForStillPendingStatus() throws Exception {
        Receipt receipt = new SingleReceipt(Status.PENDING, MESSAGE);
        final long now = System.currentTimeMillis();
        final long expected = now+100;
        receipt.waitForDelivery(100, TimeUnit.MILLISECONDS);
        final long current = System.currentTimeMillis();
        assertTrue("expected "+expected+" - current: "+current, current >= expected);
    }

    @Test
    public void shouldReturnBeforeTimeoutIfStatusChangedToDelivered() throws Exception {
        SingleReceipt receipt = new SingleReceipt(Status.PENDING, MESSAGE);
        final long now = System.currentTimeMillis();
        simulateMessageDelivered(receipt, 50);
        receipt.waitForDelivery(5, TimeUnit.SECONDS);

        assertTrue(System.currentTimeMillis() < now+1000);
    }

    @Test
    public void shouldReturnTrueIfStatusChangedToDelivered() throws Exception {
        SingleReceipt receipt = new SingleReceipt(Status.PENDING, MESSAGE);
        simulateMessageDelivered(receipt, 50);
        assertTrue(receipt.waitForDelivery(5, TimeUnit.SECONDS));
    }

    private void simulateMessageDelivered(final SingleReceipt receipt, final long delay) {
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
                receipt.setStatus(Status.DELIVERED);
            }}).start();
    }

}
