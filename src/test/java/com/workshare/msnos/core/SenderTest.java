package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.createMockCloud;
import static com.workshare.msnos.core.MessagesHelper.newPingMessage;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.receipts.SingleReceipt;
import com.workshare.msnos.core.routing.Router;

public class SenderTest {

    private Cloud cloud;
    private Sender sender;
    private Router router;
    private Executor executor;

    @Before
    public void before() throws Exception {
       cloud = createMockCloud();
       router = mock(Router.class);
       executor = mock(Executor.class);

       sender = new Sender(router, executor);
    }
    
    @Test
    public void shouldSendMessagesTroughRouter() throws Exception {
        Message message = newPingMessage(cloud);
        when(router.send(any(Message.class))).thenReturn(SingleReceipt.failure(message));

        sendAndWait(message);

        verifyMessageSent(message);
    }

    @Test
    public void shouldDecreaseHopsBeforeSending() throws Exception {
        Message message = newPingMessage(cloud);
        when(router.send(any(Message.class))).thenReturn(SingleReceipt.failure(message));
        
        sendAndWait(message);

        int expectedHops = message.getHops()-1;
        assertEquals(expectedHops, verifyMessageSent(message).getHops());
    }

    @Test
    public void shouldUpdateReceiptStatusAndGatewayWithRouterResult() throws Exception {
        final Message message = newPingMessage(cloud);
        final Receipt routerReceipt = createReceipt("FOO", Status.DELIVERED, message);
        when(router.send(any(Message.class))).thenReturn(routerReceipt);
        
        Receipt receipt = sendAndWait(message);

        assertEquals("FOO", receipt.getGate());
        assertEquals(Status.DELIVERED, receipt.getStatus());
        assertEquals(message.getUuid(), receipt.getMessageUuid());
    }

    private Message verifyMessageSent(final Message message) throws IOException {
        ArgumentCaptor<Message> runnableCaptor = ArgumentCaptor.forClass(Message.class);
        verify(router).send(runnableCaptor.capture());
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

    private Receipt createReceipt(String gateName, Status status, Message message) throws Exception {
        Gateway gate = mock(Gateway.class);
        when(gate.name()).thenReturn(gateName);
        return new SingleReceipt(gate, status, message);
    }
}
