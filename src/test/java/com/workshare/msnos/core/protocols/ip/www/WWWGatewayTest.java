package com.workshare.msnos.core.protocols.ip.www;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.serializers.WireSerializer;

public class WWWGatewayTest {

    private static final String WWW_ROOT = "https://www.wombats.com/";
    
    private static final UUID uuid1 = new UUID(111, 111);
    private static final UUID uuid2 = new UUID(222, 222);

    private WWWGateway gate;
    private HttpClient client;
    private WireSerializer serializer;
    private ScheduledExecutorService scheduler;

    @Before
    public void setup() {
        client = mock(HttpClient.class);
        scheduler = mock(ScheduledExecutorService.class);

        serializer = mock(WireSerializer.class);
        when(serializer.toBytes(anyObject())).thenAnswer(new Answer<byte[]>(){
            @Override
            public byte[] answer(InvocationOnMock invocation) throws Throwable {
                Message msg = (Message)invocation.getArguments()[0];
                return msg.getUuid().toString().getBytes();
            }} );

        System.setProperty(WWWGateway.SYSP_ADDRESS, WWW_ROOT);
        gate = new WWWGateway(client, scheduler, serializer);
    }

    @Test
    public void shouldNotSendMessagesStraightAway() throws Exception {
        gate.send(message(uuid1));
        verifyZeroInteractions(client);
    }

    @Test
    public void shouldNotSendMessagesWhenScheduled() throws Exception {
        gate.send(message(uuid1));
        gate.send(message(uuid2));
        
        scheduledTask().run();
        
        HttpPost method = getLastMessagePostedToWWW();
        assertNotNull(method);
        assertEquals(WWW_ROOT, method.getURI().toString());
    }

    private HttpPost getLastMessagePostedToWWW() throws Exception {
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(client).execute(captor.capture());
        return (HttpPost) captor.getValue();
    }

    private Runnable scheduledTask() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleAtFixedRate(captor.capture(),anyLong(),anyLong(),any(TimeUnit.class));
        return captor.getValue();
    }

    private Message message(UUID uuid) {
        final Message msg = mock(Message.class);
        when(msg.getUuid()).thenReturn(uuid);
        return msg;
    }
}
