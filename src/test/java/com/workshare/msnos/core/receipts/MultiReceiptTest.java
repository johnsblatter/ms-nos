package com.workshare.msnos.core.receipts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.cloud.LocalAgent;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.Receipt;

public class MultiReceiptTest {

    private static final Message MESSAGE = new MessageBuilder(Message.Type.PIN, new LocalAgent(UUID.randomUUID()), new LocalAgent(UUID.randomUUID())).make();

    private MultiReceipt multi;
    private Receipt[] receipts = new Receipt[3];

    @Before
    public void before() throws Exception {
        multi = new MultiReceipt(MESSAGE);

        for (int i = 0; i < receipts.length; i++) {
            receipts[i] = createMockReceipt(Status.UNKNOWN, i);
            multi.add(receipts[i]);
        }
    }

    @Test
    public void shouldReturnUnknownWhenBothUnknown() throws Exception {
        assertEquals(Status.UNKNOWN, multi.getStatus());
    }

    @Test
    public void shouldReturnFailedWhenOneFailedAndOtherUnknown() throws Exception {
        mockStatus(receipts[1], Status.FAILED);
        assertEquals(Status.FAILED, multi.getStatus());
    }

    @Test
    public void shouldReturnPendingWhenOnePending() throws Exception {
        mockStatus(receipts[0], Status.PENDING);
        mockStatus(receipts[1], Status.FAILED);
        mockStatus(receipts[2], Status.FAILED);
        assertEquals(Status.PENDING, multi.getStatus());
    }

    @Test
    public void shouldReturnDeliverdWhenOneDelivered() throws Exception {
        mockStatus(receipts[1], Status.PENDING);
        mockStatus(receipts[2], Status.DELIVERED);
        assertEquals(Status.DELIVERED, multi.getStatus());
    }

    @Test
    public void shouldWaitForTimeoutsOnAllReceipts() throws InterruptedException {
        multi.waitForDelivery(100 * receipts.length, TimeUnit.MILLISECONDS);
        for (Receipt receipt : receipts) {
            verify(receipt, atLeastOnce()).waitForDelivery(anyLong(), eq(TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void shouldReturnFalseWhenNoneDeliveredOnWaitFor() throws InterruptedException {
        boolean res = multi.waitForDelivery(100, TimeUnit.MILLISECONDS);
        assertFalse(res);
    }

    @Test
    public void shouldReturnTrueWhenOnWaitforSucceeds() throws InterruptedException {
        when(receipts[1].waitForDelivery(anyLong(), any(TimeUnit.class))).thenReturn(true);
        boolean res = multi.waitForDelivery(100, TimeUnit.MILLISECONDS);
        assertTrue(res);
    }

    @Test
    public void shouldReturnTrueWhenOnWaitforFailsButOneIsDelivered() throws InterruptedException {
        mockStatus(receipts[2], Status.DELIVERED);
        boolean res = multi.waitForDelivery(100, TimeUnit.MILLISECONDS);
        assertTrue(res);
    }

    @Test
    public void shouldWaitForTimeoutsWhenNoReceipts() throws InterruptedException {
        long now = System.currentTimeMillis();
        
        multi = new MultiReceipt(MESSAGE);
        multi.waitForDelivery(100, TimeUnit.MILLISECONDS);

        assertTrue(System.currentTimeMillis() > now + 100);
    }

    @Test
    public void shouldStoreMessageUuid() throws Exception {
        assertEquals(MESSAGE.getUuid(), multi.getMessageUuid());
    }

    @Test
    public void shouldReturnCombinedNameWhenAllUnknown() throws Exception {
        String gate = multi.getGate();
        for (Receipt receipt : receipts) {
            assertTrue(gate.contains(receipt.getGate()));
        }
    }

    @Test
    public void shouldReturnNameForTheFirstDelivered() throws InterruptedException {
        mockStatus(receipts[1], Status.DELIVERED);
        assertEquals(receipts[1].getGate(), multi.getGate());
    }



    private Receipt createMockReceipt(final Status status, int number) throws Exception {
        Receipt value = mock(Receipt.class);
        mockStatus(value, status);
        when(value.getGate()).thenReturn(Integer.toString(number));
        return value;
    }

    private void mockStatus(Receipt receipt, final Status status) {
        when(receipt.getStatus()).thenReturn(status);
    }
}
