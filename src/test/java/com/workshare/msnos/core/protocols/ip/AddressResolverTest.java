package com.workshare.msnos.core.protocols.ip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class AddressResolverTest {

    AddressResolver resolver;
    private ArgumentCaptor<HttpGet> httpMethodCaptor;

    @After
    @Before
    public void tearDown() throws Exception {
        System.clearProperty(AddressResolver.SYSP_PUBLIC_IP);
        System.clearProperty(AddressResolver.SYSP_EXTERNAL_IP);
    }

    @Test
    public void shouldUseSystemPropertyWhenDefinedOnPublicIP() throws Exception {
        System.setProperty(AddressResolver.SYSP_PUBLIC_IP, "231.132.1.2");

        resolver = new AddressResolver();
        Network result = resolver.findPublicIP();

        assertNotNull(result);
        assertEquals("231.132.1.2", result.getHostString());
    }

    @Test
    public void shouldUseHTTPClientToResolvePublicIP() throws Exception {
        HttpClient mockClient = setupMockHttpClientWithMockGetResponse();

        resolver = new AddressResolver(mockClient);
        resolver.findPublicIP();

        assertEquals(AddressResolver.AMAZON_IPV4_DISCOVERY_ENDPOINT, getInvokedUrl());
    }

    @Test
    public void shouldUseSystemPropertyWhenDefinedOnExternalIP() throws Exception {
        System.setProperty(AddressResolver.SYSP_EXTERNAL_IP, "231.132.1.9");

        resolver = new AddressResolver();
        Network result = resolver.findExternalIP();

        assertNotNull(result);
        assertEquals("231.132.1.9", result.getHostString());
    }

    @Test
    public void shouldUseHTTPClientToResolveExternalIP() throws Exception {
        HttpClient mockClient = setupMockHttpClientWithMockGetResponse();

        resolver = new AddressResolver(mockClient);
        resolver.findExternalIP();

        assertEquals(AddressResolver.AMAZON_EXTERNAL_DISCOVERY_ENDPOINT, getInvokedUrl());
    }

    private HttpClient setupMockHttpClientWithMockGetResponse() throws IOException {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse resp = mock(HttpResponse.class);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("123.2.1.2".getBytes("UTF-8")));
        when(resp.getEntity()).thenReturn(entity);
        httpMethodCaptor = ArgumentCaptor.forClass(HttpGet.class);
        when(mockClient.execute(httpMethodCaptor.capture())).thenReturn(resp);
        return mockClient;
    }

    private String getInvokedUrl() {
        try {
            return httpMethodCaptor.getValue().getURI().toString();
        } catch (Exception any) {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(new AddressResolver().findExternalIP());
    }

}
