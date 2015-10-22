package com.workshare.msnos.soup.net;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.conn.DnsResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DnsResolverWithTimeoutTest {

    private DnsResolverWithTimeout resolver;

    private DnsResolver systemResolver;
    private long timeoutInMillis;

    @Before
    public void setup() {
        systemResolver = mock(DnsResolver.class);
        timeoutInMillis = 100l;
    }

    @Test
    public void shouldReturnResolvedHostWhenWithinTimeout() throws UnknownHostException {
        InetAddress[] address = new InetAddress[]{};
        when(systemResolver.resolve(anyString())).thenReturn(address);
        
        InetAddress[] result = resolver().resolve("foo");
        
        assertArrayEquals(address, result);
    }

    @Test(expected=UnknownHostException.class)
    public void shouldThrowUnknownHostExceptionOnTimeout() throws UnknownHostException {
        when(systemResolver.resolve(anyString())).thenAnswer(new Answer<InetAddress[]>(){
            @Override
            public InetAddress[] answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(timeoutInMillis*2);
                return null;
            }});
        
        resolver().resolve("foo");
    }
    
    @Test(expected=UnknownHostException.class)
    public void shouldThrowUnknownHostExceptionOnException() throws UnknownHostException {
        when(systemResolver.resolve(anyString())).thenThrow(new RuntimeException("boom!"));

        resolver().resolve("foo");
    }
    
    @Test(expected=UnknownHostException.class)
    public void shouldThrowUnknownHostExceptionOnInterruption() throws UnknownHostException {
        when(systemResolver.resolve(anyString())).thenAnswer(new Answer<InetAddress[]>(){
            @Override
            public InetAddress[] answer(InvocationOnMock invocation) throws Throwable {
                throw new InterruptedException("NMI!");
            }});

        resolver().resolve("foo");
    }
    
    private DnsResolverWithTimeout resolver() {
        if (resolver == null)
            resolver = new DnsResolverWithTimeout(DnsResolverWithTimeout.DNS_RESOLVE_EXECUTOR, systemResolver, timeoutInMillis);
        
        return resolver;
    }
}
