package com.workshare.msnos.core.protocols.ip.resolvers;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.protocols.ip.AddressResolver;

public class IPResolverByURLTest {

    private static final String URL = "http://foo.com";
    
    private AddressResolver context;
    private IPResolverByURL resolver;

    @Before
    public void prepare() throws IOException {
        context = mock(AddressResolver.class);
        resolver = new IPResolverByURL(URL);
    }

    
    @Test
    public void shouldUseHTTPClientToResolvePublicIP() throws Exception {
        byte[] address= new byte[]{21,22,23,24};
        when(context.getIPViaURL(URL)).thenReturn(address);
        
        byte[] result = resolver.resolve(context);

        assertArrayEquals(address,  result);
    }

}
