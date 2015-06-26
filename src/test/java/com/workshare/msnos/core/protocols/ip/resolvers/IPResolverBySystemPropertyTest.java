package com.workshare.msnos.core.protocols.ip.resolvers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.protocols.ip.AddressResolver;

public class IPResolverBySystemPropertyTest {

    private static final String PROPERTY_NAME = "foo.ip";
    private AddressResolver context;

    @Before
    public void prepare() {
        context = mock(AddressResolver.class);
    }
    
    @Test
    public void shouldReturnIpBasedOnValidSystemProperty() {
        System.setProperty(PROPERTY_NAME, "21.22.23.24");
        IPResolverBySystemProperty resolver = new IPResolverBySystemProperty(PROPERTY_NAME);
        
        byte[] address = resolver.resolve(context);
        
        assertArrayEquals(new byte[]{21,22,23,24}, address);
    }
    
    @Test
    public void shouldReturnNullWhenPropertyMissing() {
        System.getProperties().remove(PROPERTY_NAME);
        IPResolverBySystemProperty resolver = new IPResolverBySystemProperty(PROPERTY_NAME);
        
        byte[] address = resolver.resolve(context);
        
        assertNull(address);
    }

    @Test
    public void shouldReturnNullWhenPropertyGibberish() {
        System.setProperty(PROPERTY_NAME, "yadda yadda");
        IPResolverBySystemProperty resolver = new IPResolverBySystemProperty(PROPERTY_NAME);
        
        byte[] address = resolver.resolve(context);
        
        assertNull(address);
    }

 }
