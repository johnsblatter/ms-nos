package com.workshare.msnos.core.protocols.ip.www;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.serializers.WireSerializer;

public class WWWGatewayTest {

    private static final String WWW_ROOT = "https://www.wombats.com/";

    private static final UUID uuid1 = new UUID(111, 111);
    private static final UUID uuid2 = new UUID(222, 222);
    private static final UUID uuid3 = new UUID(333, 333);

    private static final UUID CLOUD_UUID = UUID.randomUUID();

    private WWWGateway gate;
    private HttpClient client;
    private WireSerializer serializer;
    private ScheduledExecutorService scheduler;
    private List<Message> messages;
    private Cloud cloud;

    @Before
    public void setup() throws Exception {
        cloud = new Cloud(CLOUD_UUID);

        client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        when(client.execute(any(HttpUriRequest.class))).thenReturn(response );

        scheduler = mock(ScheduledExecutorService.class);

        serializer = mock(WireSerializer.class);
        when(serializer.toText(anyObject())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Message msg = (Message) invocation.getArguments()[0];
                final UUID uuid = msg.getUuid();
                return uuid.toString();
            }
        });

        System.setProperty(WWWGateway.SYSP_ADDRESS, WWW_ROOT);

        messages = new ArrayList<Message>();
        gate = new WWWGateway(client, scheduler, serializer);
        gate.addListener(cloud, new Listener() {
            @Override
            public void onMessage(Message message) {
                messages.add(message);
            }
        });
    }

    @Test
    public void shouldNotSendMessagesStraightAway() throws Exception {
        gate.send(message(uuid1));
        assertNull(getLastPostToWWW());
    }

    @Test
    public void shouldSendNothingWhenNoMessageAndSync() throws Exception {
        scheduledTask().run();
        assertNull(getLastPostToWWW());
    }

    @Test
    public void shouldSendOneMessageOnSync() throws Exception {
        gate.send(message(uuid1));

        scheduledTask().run();

        HttpPost request = getLastPostToWWW();
        assertEquals(WWW_ROOT, request.getURI().toString());
        assertEquals(toText(uuid1), toText(request.getEntity()));
    }

    @Test
    public void shouldSendMultipleMessageOnSync() throws Exception {
        gate.send(message(uuid1));
        gate.send(message(uuid2));
        gate.send(message(uuid3));

        scheduledTask().run();

        HttpPost request = getLastPostToWWW();
        String expected = toText(uuid1) + toText(uuid2) + toText(uuid3);
        String current = toText(request.getEntity());
        assertEquals(expected, current);
    }

    @Test
    public void shouldInvokeGetMessagesOnSync() throws Exception {
        scheduledTask().run();

        HttpGet request = getLastGetToWWW();
        assertEquals(WWW_ROOT, request.getURI().toString());
        assertParamPresent(request, "cloud", CLOUD_UUID.toString());
        
    }

    // @Test
    // public void shouldNotifyReceivedMessages() throws Exception {
    //
    // }

    private void assertParamPresent(HttpGet request, String name, String value) {
        fail("unimplemented");
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

    private String toText(HttpEntity entity) throws ParseException, IOException {
        return EntityUtils.toString(entity);
    }

    private String toText(final UUID uuid) {
        return uuid.toString() + "\n";
    }

    private HttpGet getLastGetToWWW() throws Exception {
        return getLastRequestToWWW(HttpGet.class);
    }

    private HttpPost getLastPostToWWW() throws Exception {
        return getLastRequestToWWW(HttpPost.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T getLastRequestToWWW(Class<T> type) throws Exception {
        List<HttpUriRequest> requests = getLastRequestsToWWW();
        for (HttpUriRequest request : requests) {
            if (request.getClass() == type) {
                return (T) request;
            }
        }
        
        return null;
    }
    private List<HttpUriRequest> getLastRequestsToWWW() throws IOException, ClientProtocolException {
        try {
            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(client, atLeastOnce()).execute(captor.capture());
            return captor.getAllValues();
        } catch (Throwable any) {
            return Collections.<HttpUriRequest>emptyList() ;
        }
    }

}
