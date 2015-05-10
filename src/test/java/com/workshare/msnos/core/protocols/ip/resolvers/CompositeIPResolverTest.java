package com.workshare.msnos.core.protocols.ip.resolvers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.workshare.msnos.core.protocols.ip.AddressResolver;

public class CompositeIPResolverTest {

    private AddressResolver context;

    @Before
    public void prepare() throws IOException {
        context = mock(AddressResolver.class);
    }
    
    @Test
    public void shouldInvokeAllResolversInOrder() {
        IPResolver one = mock(IPResolver.class);
        IPResolver two = mock(IPResolver.class);

        new CompositeIPResolver(one, two).resolve(context);
        
        InOrder inOrder = Mockito.inOrder(one, two);
        inOrder.verify(one).resolve(context);
        inOrder.verify(two).resolve(context);
    }
    
    @Test
    public void shouldReturnTheFirstSuccess() {
        byte[] expected = new byte[]{};
        IPResolver one = mock(IPResolver.class);
        IPResolver two = mock(IPResolver.class);
        IPResolver tre = mock(IPResolver.class);
        when(two.resolve(context)).thenReturn(expected);
        
        byte[] result = new CompositeIPResolver(one, two, tre).resolve(context);
        
        verify(one).resolve(context);
        verify(two).resolve(context);
        verifyZeroInteractions(tre);
        assertEquals(expected, result);
    }

    @Test
    public void shouldNotExplodeIfOneResolverBombs() {
        byte[] expected = new byte[]{};
        IPResolver one = mock(IPResolver.class);
        IPResolver two = mock(IPResolver.class);
        IPResolver tre = mock(IPResolver.class);
        when(two.resolve(context)).thenThrow(new RuntimeException("Boom!"));
        when(tre.resolve(context)).thenReturn(expected);
        
        byte[] result = new CompositeIPResolver(one, two, tre).resolve(context);
        
        assertEquals(expected, result);
    }
}
