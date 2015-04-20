package com.workshare.msnos.core.receipts;

import static com.workshare.msnos.core.CoreHelper.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.Receipt;

public class DelegateReceiptTest {

    private static final Message MESSAGE = new MessageBuilder(Message.Type.PIN, new LocalAgent(UUID.randomUUID()), new LocalAgent(UUID.randomUUID())).make();

    private DelegateReceipt receipt;

    @Before
    public void before() throws Exception {
        receipt = new DelegateReceipt(MESSAGE);
    }

    @Test
    public void shouldStoreMessageUuid() throws Exception {
        assertEquals(MESSAGE.getUuid(), receipt.getMessageUuid());
    }

    @Test
    public void shouldReturnUnknownWhenNoDelegate() throws Exception {
        assertEquals(Status.UNKNOWN, receipt.getStatus());
    }
    
    @Test
    public void shouldWaitForTimeoutsWhenNoDelegate() throws Exception {
        long now = System.currentTimeMillis();
        
        receipt.waitForDelivery(100, TimeUnit.MILLISECONDS);

        assertTrue(System.currentTimeMillis() > now + 100);
    }
    
    @Test
    public void shouldReturnFailedWhenDelegateFailed() throws Exception {
        Receipt delegate = createMockReceipt(Status.FAILED);

        receipt.setDelegate(delegate);
        
        assertEquals(Status.FAILED, receipt.getStatus());
    }
    
    @Test
    public void shouldReturnPendingWhenDelegatePending() throws Exception {
        Receipt delegate = createMockReceipt(Status.PENDING);

        receipt.setDelegate(delegate);
        
        assertEquals(Status.PENDING, receipt.getStatus());
    }
    
    @Test
    public void shouldReturnDeliveredWhenDelegateDelivered() throws Exception {
        Receipt delegate = createMockReceipt(Status.DELIVERED);

        receipt.setDelegate(delegate);
        
        assertEquals(Status.DELIVERED, receipt.getStatus());
    }
    
    @Test
    public void shouldWaitForTimeoutStopWhenNoDelegateInjectedAndDelivered() throws Exception {
        long now = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                sleep(150, TimeUnit.MILLISECONDS);
                receipt.setDelegate(createMockReceipt(Status.DELIVERED));
            }}).start();
        
        receipt.waitForDelivery(300, TimeUnit.MILLISECONDS);

        long elapsed = System.currentTimeMillis() - now;
        assertTrue(elapsed > 100);
        assertTrue(elapsed < 200);
    }
    

/*
    @Test
    public void shouldWaitForTimeoutsOnAllReceipts() throws InterruptedException {
        receipt.waitForDelivery(100 * receipts.length, TimeUnit.MILLISECONDS);
        for (Receipt receipt : receipts) {
            verify(receipt, atLeastOnce()).waitForDelivery(anyLong(), eq(TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void shouldReturnFalseWhenNoneDeliveredOnWaitFor() throws InterruptedException {
        boolean res = receipt.waitForDelivery(100, TimeUnit.MILLISECONDS);
        assertFalse(res);
    }

    @Test
    public void shouldReturnTrueWhenOnWaitforSucceeds() throws InterruptedException {
        when(receipts[1].waitForDelivery(anyLong(), any(TimeUnit.class))).thenReturn(true);
        boolean res = receipt.waitForDelivery(100, TimeUnit.MILLISECONDS);
        assertTrue(res);
    }

    @Test
    public void shouldReturnTrueWhenOnWaitforFailsButOneIsDelivered() throws InterruptedException {
        mockStatus(receipts[2], Status.DELIVERED);
        boolean res = receipt.waitForDelivery(100, TimeUnit.MILLISECONDS);
        assertTrue(res);
    }


    @Test
    public void shouldReturnCombinedNameWhenAllUnknown() throws Exception {
        String gate = receipt.getGate();
        for (Receipt receipt : receipts) {
            assertTrue(gate.contains(receipt.getGate()));
        }
    }

    @Test
    public void shouldReturnNameForTheFirstDelivered() throws InterruptedException {
        mockStatus(receipts[1], Status.DELIVERED);
        assertEquals(receipts[1].getGate(), receipt.getGate());
    }

*/


    private Receipt createMockReceipt(final Status status) {
        Receipt value = mock(Receipt.class);
        mockStatus(value, status);
        when(value.getGate()).thenReturn("FOO");
        return value;
    }

    private void mockStatus(Receipt receipt, final Status status) {
        when(receipt.getStatus()).thenReturn(status);
    }
}
