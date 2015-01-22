package com.workshare.msnos.usvc.api;

import static org.junit.Assert.*;

import org.junit.Test;

import com.workshare.msnos.usvc.api.RestApi.Type;

public class RestApiTest {
    
    @Test
    public void shouldImplementHashcodeAndEqualsWhenAllSame() {
        RestApi one = new RestApi("name", "path", 8888, "host", Type.INTERNAL, false);
        RestApi two = new RestApi("name", "path", 8888, "host", Type.INTERNAL, false);
        assertEquals(one.hashCode(), two.hashCode());
        assertEquals(one, two);
    }

    @Test
    public void shouldImplementHashCodeAndEqualsWhenHostDifferent() {
        RestApi one = new RestApi("name", "path", 8888, "hostA", Type.INTERNAL, false);
        RestApi two = new RestApi("name", "path", 8888, "hostB", Type.INTERNAL, false);
        assertNotEquals(one.hashCode(), two.hashCode());
        assertNotEquals(one, two);
    }
    
    @Test
    public void shouldGetUrlWorkAsExpected() {
        assertEquals("http://host:8888/path", new RestApi("name", "path", 8888, "host").getUrl());
        assertEquals("http://host:8888/path", new RestApi("name", "/path", 8888, "host").getUrl());
    }
    
}
