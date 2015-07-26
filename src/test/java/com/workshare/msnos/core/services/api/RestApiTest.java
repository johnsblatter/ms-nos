package com.workshare.msnos.core.services.api;

import static org.junit.Assert.*;

import org.junit.Test;

import com.workshare.msnos.core.services.api.RestApi;
import com.workshare.msnos.core.services.api.RestApi.Type;

public class RestApiTest {
    
    @Test
    public void shouldImplementHashcodeAndEqualsWhenAllSame() {
        RestApi one = new RestApi("path", 8888, "host", Type.INTERNAL, false);
        RestApi two = new RestApi("path", 8888, "host", Type.INTERNAL, false);
        assertEquals(one.hashCode(), two.hashCode());
        assertEquals(one, two);
    }

    @Test
    public void shouldImplementHashCodeAndEqualsWhenHostDifferent() {
        RestApi one = new RestApi("path", 8888, "hostA", Type.INTERNAL, false);
        RestApi two = new RestApi("path", 8888, "hostB", Type.INTERNAL, false);
        assertNotEquals(one.hashCode(), two.hashCode());
        assertNotEquals(one, two);
    }
    
    @Test
    public void shouldGetUrlWorkAsExpected() {
        assertEquals("http://host:8888/path", new RestApi("path", 8888, "host").getUrl());
        assertEquals("http://host:8888/path", new RestApi("/path", 8888, "host").getUrl());
    }
    
}
