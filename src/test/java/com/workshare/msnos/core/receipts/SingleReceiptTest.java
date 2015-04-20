package com.workshare.msnos.core.receipts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.protocols.ip.NullGateway;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.Receipt;

public class SingleReceiptTest {

    private static final Message MESSAGE = new MessageBuilder(Message.Type.PIN, new LocalAgent(UUID.randomUUID()), new LocalAgent(UUID.randomUUID())).make();

    private Gateway gate;

    @Before
    public void prepare() {
        gate = new NullGateway();
    }
    
    @Test
    public void shouldStoreMessageUuid() throws Exception {
        Receipt receipt = new SingleReceipt(gate, Status.UNKNOWN, MESSAGE);
        assertEquals(MESSAGE.getUuid(), receipt.getMessageUuid());
    }

    @Test
    public void shouldAwaitReturnFalseForUnknownStatus() throws Exception {
        Receipt receipt = new SingleReceipt(gate, Status.UNKNOWN, MESSAGE);
        assertFalse(receipt.waitForDelivery(1, TimeUnit.SECONDS));
    }

    @Test
    public void shouldAwaitReturnImmediatelyForUnknownStatus() throws Exception {
        Receipt receipt = new SingleReceipt(gate, Status.UNKNOWN, MESSAGE);
        long now = System.currentTimeMillis();
        receipt.waitForDelivery(1, TimeUnit.SECONDS);
        assertEquals(now/10, System.currentTimeMillis()/10, 0.1);
    }
    
    @Test
    public void shouldAwaitReturnImmediatelyForFailedStatus() throws Exception {
        Receipt receipt = new SingleReceipt(gate, Status.FAILED, MESSAGE);
        long now = System.currentTimeMillis();
        receipt.waitForDelivery(1, TimeUnit.SECONDS);
        assertEquals(now/10, System.currentTimeMillis()/10, 0.1);
    }
    
    @Test
    public void shouldAwaitReturnTrueForDeliveredStatus() throws Exception {
        Receipt receipt = new SingleReceipt(gate, Status.DELIVERED, MESSAGE);
        assertTrue(receipt.waitForDelivery(1, TimeUnit.SECONDS));
    }

    @Test
    public void shouldAwaitReturnImmediatelyForDeliveredStatus() throws Exception {
        Receipt receipt = new SingleReceipt(gate, Status.DELIVERED, MESSAGE);
        long now = System.currentTimeMillis();
        receipt.waitForDelivery(1, TimeUnit.SECONDS);
        assertEquals(now/10, System.currentTimeMillis()/10, 0.1);
    }

    @Test
    public void shouldAwaitReturnFalseForDeliveredStatusStillPending() throws Exception {
        Receipt receipt = new SingleReceipt(gate, Status.PENDING, MESSAGE);
        assertFalse(receipt.waitForDelivery(10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldAwaitReturnAfterTimeoutForStillPendingStatus() throws Exception {
        Receipt receipt = new SingleReceipt(gate, Status.PENDING, MESSAGE);
        final long now = System.currentTimeMillis();
        final long expected = now+100;
        receipt.waitForDelivery(100, TimeUnit.MILLISECONDS);
        final long current = System.currentTimeMillis();
        assertTrue("expected "+expected+" - current: "+current, current >= expected);
    }

    @Test
    public void shouldReturnBeforeTimeoutIfStatusChangedToDelivered() throws Exception {
        SingleReceipt receipt = new SingleReceipt(gate, Status.PENDING, MESSAGE);
        final long now = System.currentTimeMillis();
        simulateMessageChange(receipt, 50, Status.DELIVERED);
        receipt.waitForDelivery(5, TimeUnit.SECONDS);

        assertTrue(System.currentTimeMillis() < now+1000);
    }

    @Test
    public void shouldReturnTrueIfStatusChangedToDelivered() throws Exception {
        SingleReceipt receipt = new SingleReceipt(gate, Status.PENDING, MESSAGE);
        simulateMessageChange(receipt, 50, Status.DELIVERED);
        assertTrue(receipt.waitForDelivery(5, TimeUnit.SECONDS));
    }

    @Test
    public void shouldStoreGatewayName() throws Exception {
        SingleReceipt receipt = new SingleReceipt(gate, Status.PENDING, MESSAGE);
        assertEquals(gate.name(), receipt.getGate());
    }

    @Test
    public void shouldReturnBeforeTimeoutIfStatusChangedToFailed() throws Exception {
        SingleReceipt receipt = new SingleReceipt(gate, Status.PENDING, MESSAGE);
        final long now = System.currentTimeMillis();
        simulateMessageChange(receipt, 50, Status.FAILED);
        receipt.waitForDelivery(5, TimeUnit.SECONDS);

        assertTrue(System.currentTimeMillis() < now+1000);
    }

    @Test
    public void shouldReturnFalseIfStatusChangedToFailed() throws Exception {
        SingleReceipt receipt = new SingleReceipt(gate, Status.PENDING, MESSAGE);
        simulateMessageChange(receipt, 50, Status.FAILED);
        assertFalse(receipt.waitForDelivery(5, TimeUnit.SECONDS));
    }

    private void simulateMessageChange(final SingleReceipt receipt, final long delay, Status status) {
        final SingleReceipt newReceipt = new SingleReceipt(gate, status, MESSAGE);
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
                receipt.update(newReceipt);
            }}).start();
    }

}
