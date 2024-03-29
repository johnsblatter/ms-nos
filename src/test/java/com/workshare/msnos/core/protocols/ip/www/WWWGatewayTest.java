package com.workshare.msnos.core.protocols.ip.www;

import static com.workshare.msnos.core.CoreHelper.synchronousGatewayMulticaster;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.serializers.WireJsonSerializer;
import com.workshare.msnos.core.serializers.WireSerializer;

public class WWWGatewayTest {

    private static final String WWW_ROOT = "https://www.wombats.com/";

    private static final UUID uuid1 = new UUID(111, 111);
    private static final UUID uuid2 = new UUID(222, 222);
    private static final UUID uuid3 = new UUID(333, 333);

    private static final UUID CLOUD_UUID = UUID.randomUUID();

    private WWWGateway gate;
    private WireSerializer serializer;
    private ScheduledExecutorService scheduler;
    private List<Message> rxMessages;
    private Cloud cloud;
    private HttpClientHelper http;

    private WWWSynchronizer synchro;
    private WWWSynchronizer.Processor processor;
    
    @Before
    public void setup() throws Exception {
        
        cloud = new Cloud(CLOUD_UUID, " ", Collections.<Gateway>emptySet());

        System.setProperty(WWWGateway.SYSP_ADDRESS, WWW_ROOT);

        http = new HttpClientHelper();

        scheduler = mock(ScheduledExecutorService.class);
        serializer = mockWireSerializer();

        rxMessages = new ArrayList<Message>();

        synchro = mock(WWWSynchronizer.class);
        processor = mock(WWWSynchronizer.Processor.class);
        when(synchro.init(any(Cloud.class))).thenReturn(processor);
        
        gate = new WWWGateway(client(), synchro , scheduler, serializer, synchronousGatewayMulticaster());
        gate.addListener(cloud, new Listener() {
            @Override
            public void onMessage(Message message) {
                rxMessages.add(message);
            }
        });
    }

    @Test
    public void shouldNotSendMessagesStraightAway() throws Exception {
        gate.send(cloud, message(uuid1), null);
        assertNull(http.getLastPostToWWW());
    }

    @Test
    public void shouldReturnAReceiptOnSend() throws Exception {
        Receipt receipt = gate.send(cloud, message(uuid1), null);
        assertNotNull(receipt);
        assertEquals(uuid1, receipt.getMessageUuid());
        assertEquals(Status.PENDING, receipt.getStatus());
        assertEquals("WWW", receipt.getGate());
    }

    @Test
    public void shouldSendNothingWhenNoMessageAndSync() throws Exception {
        scheduledTask().run();
        assertNull(http.getLastPostToWWW());
    }

    @Test
    public void shouldSendOneMessageOnSync() throws Exception {
        gate.send(cloud, message(uuid1), null);

        scheduledTask().run();

        HttpPost request = http.getLastPostToWWW();
        assertEquals(messagesRequestUrl(cloud), request.getURI().toString());
        assertEquals(toText(uuid1), toText(request.getEntity()));
    }

    @Test
    public void shouldSendMultipleMessageOnSync() throws Exception {
        gate.send(cloud, message(uuid1), null);
        gate.send(cloud, message(uuid2), null);
        gate.send(cloud, message(uuid3), null);

        scheduledTask().run();

        HttpPost request = http.getLastPostToWWW();
        String expected = toText(uuid1) + toText(uuid2) + toText(uuid3);
        String current = toText(request.getEntity());
        assertEquals(expected, current);
    }


    @Test
    public void shouldExecuteTwoDifferentHttpCallsWhenSendingMessagesForTwoClouds() throws Exception {
        Cloud otherCloud = mock(Cloud.class);
        when(otherCloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, UUID.randomUUID()));

        gate.send(cloud, message(uuid1), null);
        gate.send(otherCloud, message(uuid2), null);

        scheduledTask().run();

        List<HttpPost> requests = http.getAllRequestToWWW(HttpPost.class);
        assertEquals(2, requests.size());
        http.assertRequestsContains(requests, messagesRequestUrl(cloud));
        http.assertRequestsContains(requests, messagesRequestUrl(otherCloud));

    }

    @Test
    public void shouldInvokeGetMessagesOnSync() throws Exception {
        scheduledTask().run();

        HttpGet request = http.getLastGetToWWW();
        assertEquals(messagesRequestUrl(cloud), request.getURI().toString());

    }

    @Test
    public void shouldInvokeGetMessagesOnSyncStartingFromTheLastOne() throws Exception {
        final Message message = new MessageBuilder(Message.Type.PIN, cloud, cloud).make();
        mockGetResponse(message);

        scheduledTask().run();
        scheduledTask().run();

        HttpGet request = http.getLastGetToWWW();
        assertEquals(messagesRequestUrl(cloud, message), request.getURI().toString());
    }

    @Test
    public void shouldInvokeProcessorOnReceivedMessagesTheVeryFirstTime() throws Exception {
        final Message ping = new MessageBuilder(Message.Type.PIN, cloud, cloud).make();
        final Message pong = new MessageBuilder(Message.Type.PON, cloud, cloud).make();
        mockGetResponse(ping, pong);

        scheduledTask().run();

        assertEquals(0, rxMessages.size());
        verify(synchro).init(cloud);
        verify(processor).accept(ping);
        verify(processor).accept(pong);
        verify(processor).commit();
    }

    @Test
    public void shouldInvokeListenerOnReceivedMessagesTheSecondTime() throws Exception {
        scheduledTask().run();
        Mockito.reset(synchro);
        mockGetResponse(new MessageBuilder(Message.Type.PIN, cloud, cloud).make());

        scheduledTask().run();

        assertEquals(1, rxMessages.size());
        verifyZeroInteractions(synchro);
    }

    @Test
    public void shouldAvoidConcurrentSync() throws Exception {
        final AtomicInteger runs = new AtomicInteger(0);
        when(client().execute(any(HttpUriRequest.class))).thenAnswer(new Answer<HttpResponse>() {
            @Override
            public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                runs.incrementAndGet();
                sleep(250l);
                return response();
            }
        });

        waitFor(asyncRun(scheduledTask()), asyncRun(scheduledTask()), asyncRun(scheduledTask()));

        assertEquals(1, runs.get());
    }

    @Test(expected = IOException.class)
    public void shouldBlowUpIfCannotContactTheServer() throws Exception {
        mockExceptionResponse();
        gate = new WWWGateway(client(), scheduler, serializer, synchronousGatewayMulticaster());
    }

    @Test
    public void shouldInvokeGetMessagesOnSyncRestartingAfterConsecutiveErrors() throws Exception {
        final Message message = new MessageBuilder(Message.Type.PIN, cloud, cloud).make();
        mockGetResponse(message);
        scheduledTask().run();

        mockExceptionResponse();
        for (int i=0; i<WWWGateway.MAX_TOTAL_CONSECUTIVE_ERRORS; i++)
            scheduledTask().run();
        scheduledTask().run();

        HttpGet request = http.getLastGetToWWW();
        assertEquals(messagesRequestUrl(cloud), request.getURI().toString());
    }

    @Test
    public void shouldInvokeGetMessagesOnSyncStartingFromTheLastOneIfNotEnoughConsecutiveErrors() throws Exception {
        final Message message = new MessageBuilder(Message.Type.PIN, cloud, cloud).make();

        for (int i=0; i<WWWGateway.MAX_TOTAL_CONSECUTIVE_ERRORS; i++) {
            http.reset();
            mockExceptionResponse();
            scheduledTask().run();

            http.reset();
            mockGetResponse(message);
            scheduledTask().run();
        }

        HttpGet request = http.getLastGetToWWW();
        assertEquals(messagesRequestUrl(cloud, message), request.getURI().toString());
    }


    private void mockExceptionResponse() throws IOException, ClientProtocolException {
        when(client().execute(any(HttpUriRequest.class))).thenThrow(new IOException("boom!"));
    }

    private void mockGetResponse(Message... messages) throws UnsupportedEncodingException {
        StringBuilder input = new StringBuilder();
        for (Message message : messages) {
            input.append(toWireJson(message));
            input.append("\n");
        }

        when(response().getEntity()).thenReturn(new StringEntity(input.toString()));
    }

    private void waitFor(Thread... threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private Thread asyncRun(final Runnable task) {
        final Thread t = new Thread(task);
        t.start();
        return t;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    private Runnable scheduledTask() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleAtFixedRate(captor.capture(), anyLong(), anyLong(), any(TimeUnit.class));
        return captor.getValue();
    }

    private Message message(UUID uuid) {
        final Message msg = mock(Message.class);
        when(msg.getUuid()).thenReturn(uuid);
        return msg;
    }

    private String toText(final UUID uuid) {
        return uuid.toString() + "\n";
    }

    public String toText(HttpEntity entity) throws ParseException, IOException {
        return EntityUtils.toString(entity);
    }

    private WireSerializer mockWireSerializer() {
        WireSerializer serializer = mock(WireSerializer.class);
        when(serializer.toText(anyObject())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Message msg = (Message) invocation.getArguments()[0];
                final UUID uuid = msg.getUuid();
                return uuid.toString();
            }
        });

        Answer<Message> answer = new Answer<Message>() {
            @Override
            public Message answer(InvocationOnMock invocation) throws Throwable {
                String text = (String) invocation.getArguments()[0];
                return fromWireJson(text);
            }
        };
        when(serializer.fromText(anyString(), (Class<?>) any())).thenAnswer(answer);
        return serializer;
    }

    private String toWireJson(Message message) {
        return new WireJsonSerializer().toText(message);
    }

    private Message fromWireJson(String text) {
        return new WireJsonSerializer().fromText(text, Message.class);
    }

    private String messagesRequestUrl(final Cloud cloud) {
        return messagesRequestUrl(cloud, null);
    }

    private String messagesRequestUrl(final Cloud cloud, final Message message) {
        String url = WWW_ROOT + "api/1.0/messages?cloud=" + cloud.getIden().getUUID();
        if (message != null)
            url = url + "&message=" + message.getUuid().toString();
        return url;
    }

    private HttpClient client() {
        return http.client();
    }

    private HttpResponse response() {
        return http.response();
    }

}
