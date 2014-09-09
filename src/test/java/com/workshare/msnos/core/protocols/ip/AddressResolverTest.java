package com.workshare.msnos.core.protocols.ip;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Created by rhys on 08/09/14.
 */
public class AddressResolverTest {

    AddressResolver resolver;

    @After
    public void tearDown() throws Exception {
        System.clearProperty("public.ip");
    }

    @Test
    public void shouldUseSystemPropertyWhenDefined() throws Exception {
        System.setProperty("public.ip", "231.132.1.2");

        resolver = new AddressResolver();
        Network result = resolver.findPublicIP();

        assertEquals("231.132.1.2", result.getHostString());
    }

    @Test
    public void shouldUseHTTPClientToReachAWS() throws Exception {
        HttpClient mockClient = setupMockHttpClientWithMockGetResponse();

        resolver = new AddressResolver(mockClient);
        resolver.findPublicIP();

        verify(mockClient, atLeastOnce()).execute(any(HttpGet.class));
    }

    private HttpClient setupMockHttpClientWithMockGetResponse() throws IOException {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse resp = mock(HttpResponse.class);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("123.2.1.2".getBytes("UTF-8")));
        when(resp.getEntity()).thenReturn(entity);
        when(mockClient.execute(any(HttpGet.class))).thenReturn(resp);
        return mockClient;
    }


}
