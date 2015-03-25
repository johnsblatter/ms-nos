package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.newCloudIden;
import static com.workshare.msnos.core.MessagesHelper.newPingMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.workshare.msnos.core.Message.Status;

public class SenderTest {

    private Cloud cloud;
    private Receipt unknownReceipt;
    private Gateway gate1;
    private Gateway gate2;
    private LinkedHashSet<Gateway> gates;
    private Sender sender;
    private Executor executor;

    @Before
    public void before() throws Exception {
       unknownReceipt = mock(Receipt.class);
       when(unknownReceipt.getStatus()).thenReturn(Status.UNKNOWN);

       gate1 = mock(Gateway.class);
       when(gate1.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(unknownReceipt);
       gate2 = mock(Gateway.class);
       when(gate2.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(unknownReceipt);
       gates = new LinkedHashSet<Gateway>(Arrays.asList(gate1, gate2));

       cloud = mock(Cloud.class); 
       when(cloud.getGateways()).thenReturn(gates);
       when(cloud.getIden()).thenReturn(newCloudIden());

       executor = mock(Executor.class);

       sender = new Sender(executor);
    }
    
    @Test
    public void shouldSendMessagesTroughAllGateways() throws Exception {
        Message message = newPingMessage(cloud);
        
        sendAndWait(message);

        verifyMessageSent(message, gate1);
        verifyMessageSent(message, gate2);
    }

    @Test
    public void shouldDecreaseHopsBeforeSending() throws Exception {
        Message message = newPingMessage(cloud);
        
        sendAndWait(message);

        int expectedHops = message.getHops()-1;
        assertEquals(expectedHops, verifyMessageSent(message, gate1).getHops());
        assertEquals(expectedHops, verifyMessageSent(message, gate2).getHops());
    }

    @Test
    public void shouldNotSendMessageOnZeroHops() throws Exception {
        Message message = new MessageBuilder(Message.Type.PIN, cloud.getIden(), cloud.getIden()).withHops(0).make();
        Receipt receipt = sender.send(cloud, message);
        
        verify(gate1, never()).send(eq(cloud), any(Message.class), any(Identifiable.class));
        verify(gate2, never()).send(eq(cloud), any(Message.class), any(Identifiable.class));
        assertEquals(Message.Status.FAILED, receipt.getStatus());
    }

    @Test
    public void shouldSendMessagesTroughAllGatewaysUnlessDelivered() throws Exception {
        Receipt okayReceipt = mock(Receipt.class);
        when(okayReceipt.getStatus()).thenReturn(Status.DELIVERED);
        when(gate1.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(okayReceipt);
        
        Message message = newPingMessage(cloud);
        sendAndWait(message);

        verifyMessageSent(message, gate1);
        verify(gate2, never()).send(eq(cloud), eq(message), any(Identifiable.class));
    }

    @Test
    public void shouldNotThrowExceptionWhenSendFailedOnSomeGateways() throws Exception {
        Receipt value1 = createMockReceipt(Status.UNKNOWN);
        when(gate1.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(value1);
        when(gate2.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenThrow(new IOException("boom"));

        Message message = newPingMessage(cloud);
        Receipt receipt = sendAndWait(message);

        MultiReceipt multi = (MultiReceipt) receipt;
        assertTrue(multi.getReceipts().contains(value1));
        assertEquals(gates.size()-1, multi.getReceipts().size());
    }


    @Test
    public void shouldReturnFailureIfAllGatewaysBombs() throws Exception {
        when(gate1.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenThrow(new RuntimeException("boom!"));
        when(gate2.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenThrow(new RuntimeException("boom!"));
        
        Message message = newPingMessage(cloud);
        Receipt receipt = sendAndWait(message);
        
        assertEquals(Message.Status.FAILED, receipt.getStatus());
    }

    @Test
    public void shouldSendReturnMultipleStatusWhenUsingMultipleGateways() throws Exception {
        Receipt value1 = createMockReceipt(Status.UNKNOWN);
        when(gate1.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(value1);

        Receipt value2 = createMockReceipt(Status.UNKNOWN);
        when(gate2.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(value2);

        Message message = newPingMessage(cloud);
        Receipt receipt = sendAndWait(message);
        assertEquals(MultiReceipt.class, receipt.getClass());

        MultiReceipt multi = (MultiReceipt) receipt;
        assertTrue(multi.getReceipts().contains(value1));
        assertTrue(multi.getReceipts().contains(value2));
    }

    @Test
    public void shouldSendReturnUnknownStatusWhenUnreliable() throws Exception {
        Message message = newPingMessage(cloud);
        Receipt receipt = sendAndWait(message);
        assertEquals(Status.UNKNOWN, receipt.getStatus());
    }

    private Message verifyMessageSent(final Message message, final Gateway gate) throws IOException {
        ArgumentCaptor<Message> runnableCaptor = ArgumentCaptor.forClass(Message.class);
        verify(gate).send(eq(cloud), runnableCaptor.capture(), any(Identifiable.class));
        Message captured = runnableCaptor.getValue();
        assertEquals(message.getUuid(), captured.getUuid());
        return captured;
    }

    private Receipt sendAndWait(Message message) throws MsnosException {
        Receipt receipt = sender.send(cloud, message);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        
        return receipt;
    }

    private Receipt createMockReceipt(final Status status) throws InterruptedException, ExecutionException {
        Receipt value = Mockito.mock(Receipt.class);
        when(value.getStatus()).thenReturn(status);
        return value;
    }
}
