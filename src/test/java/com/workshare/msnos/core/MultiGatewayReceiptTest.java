package com.workshare.msnos.core;

import com.workshare.msnos.core.Message.Status;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MultiGatewayReceiptTest {

    private static final Message MESSAGE = Messages.ping(new LocalAgent(UUID.randomUUID()), new LocalAgent(UUID.randomUUID()));

    private MultiGatewayReceipt multi;
    private Receipt[] receipts = new Receipt[3];

    @Before
    public void before() throws Exception {
        multi = new MultiGatewayReceipt(MESSAGE);

        for (int i = 0; i < receipts.length; i++) {
            receipts[i] = createMockReceipt(Status.UNKNOWN);
            multi.add(receipts[i]);
        }
    }

    @Test
    public void shouldReturnUnknownWhenBothUnknown() throws Exception {
        assertEquals(Status.UNKNOWN, multi.getStatus());
    }

    @Test
    public void shouldReturnPendingWhenOnePending() throws Exception {
        mockStatus(receipts[1], Status.PENDING);
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
            verify(receipt).waitForDelivery(100, TimeUnit.MILLISECONDS);
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


    private Receipt createMockReceipt(final Status status) throws Exception {
        Receipt value = mock(Receipt.class);
        mockStatus(value, status);
        return value;
    }

    private void mockStatus(Receipt receipt, final Status status) {
        when(receipt.getStatus()).thenReturn(status);
    }

}
