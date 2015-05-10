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


// FIXME TODO refactor this test and remove the obsolete methods as now everythng 
// is running trough resolvers
public class AddressResolverTest {

    private AddressResolver resolver;
    private ArgumentCaptor<HttpGet> httpMethodCaptor;

    @After
    @Before
    public void tearDown() throws Exception {
        System.clearProperty(AddressResolver.SYSP_PUBLIC_IP);
        System.clearProperty(AddressResolver.SYSP_ROUTER_IP);
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

        resolver = new AddressResolver(mockClient, AddressResolver.FOR_ROUTER_IP, AddressResolver.FOR_PUBLIC_IP);
        resolver.findPublicIP();

        assertEquals(AddressResolver.AMAZON_IPV4_DISCOVERY_ENDPOINT, getInvokedUrl());
    }

    @Test
    public void shouldUseSystemPropertyWhenDefinedOnRouterIP() throws Exception {
        System.setProperty(AddressResolver.SYSP_ROUTER_IP, "231.132.1.9");

        resolver = new AddressResolver();
        Network result = resolver.findRouterIP();

        assertNotNull(result);
        assertEquals("231.132.1.9", result.getHostString());
    }

    @Test
    public void shouldUseHTTPClientToResolveRouterIP() throws Exception {
        HttpClient mockClient = setupMockHttpClientWithMockGetResponse();

        resolver = new AddressResolver(mockClient, AddressResolver.FOR_ROUTER_IP, AddressResolver.FOR_PUBLIC_IP);
        resolver.findRouterIP();

        assertEquals(AddressResolver.ROUTER_DISCOVERY_ENDPOINT, getInvokedUrl());
    }

    @Test
    public void shouldUseRouterResolverWhenRequestedForPublicIP() throws Exception {
        System.setProperty(AddressResolver.SYSP_PUBLIC_IP, "router");

        AddressResolver resolver = new AddressResolver();

        assertEquals(AddressResolver.FOR_ROUTER_IP, resolver.getPublicIpResolver());
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
        System.out.println("Public: "+new AddressResolver().findPublicIP());
        System.out.println("Router: "+new AddressResolver().findRouterIP());
    }

}
