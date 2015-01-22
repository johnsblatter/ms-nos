package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.fakeSystemTime;
import static com.workshare.msnos.core.CoreHelper.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.soup.time.SystemTime;

public class RemoteAgentTest {

    private Cloud cloud;

    @Before
    public void before() throws Exception {
        cloud = mock(Cloud.class);
    }

    @After
    public void after() throws Exception {
        SystemTime.reset();
    }

    @Test
    public void shouldTouchWhenCreated() {
        fakeSystemTime(12345L);
        
        Set<Endpoint> endpoints = Collections.emptySet();
        RemoteAgent agent = new RemoteAgent(randomUUID(), cloud, endpoints);
        
        assertEquals(12345L, agent.getAccessTime());
    }
}
