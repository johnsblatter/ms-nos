package com.workshare.msnos.soup.time;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.net.time.TimeTCPClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TimeTCPClient.class})
public class NTPClientTest {

    private TimeTCPClient client;

    @Before 
    public void prepare() {
        client = mock(TimeTCPClient.class);
        when(client.isConnected()).thenReturn(true);
    }
    
    @Test   
    public void shouldConnectAndDisconnectToHost() throws Exception {
        NTPClient ntp = new NTPClient(Arrays.asList("10.10.10.10"), client);
        try {ntp.getTime();}
        catch (Exception ignore) {}
        
        verify(client).connect("10.10.10.10");
        verify(client).disconnect();
    }

    @Test(expected=IOException.class)   
    public void shouldFailIfClientNotConnected() throws Exception {
        when(client.isConnected()).thenReturn(false);
       when(client.getTime()).thenReturn(1234L);

        NTPClient ntp = new NTPClient(Arrays.asList("10.10.10.10"), client);
        ntp.getTime();
    }

    @Test   
    public void shouldReturnTimeFromTimeTCPClient() throws Exception {
        when(client.getTime()).thenReturn(1234L);
        when(client.isConnected()).thenReturn(true);

        NTPClient ntp = new NTPClient(Arrays.asList("10.10.10.10"), client);
        long time = ntp.getTime();
        
        assertEquals(1234L, time);
    }

    @Test   
    public void shouldAskToAllSources() throws Exception {
        when(client.isConnected()).thenReturn(false);

        NTPClient ntp = new NTPClient(Arrays.asList(new String[]{"alfa", "beta", "gamma"}), client);
        try {ntp.getTime();}
        catch (Exception ignore) {}
        
        verify(client).connect("alfa");
        verify(client).connect("beta");
        verify(client).connect("gamma");
    }

    @Test   
    public void shouldAskToAllSourcesOnExceptions() throws Exception {
        doThrow(new IOException("boom!")).when(client).connect(anyString());

        NTPClient ntp = new NTPClient(Arrays.asList(new String[]{"alfa", "beta", }), client);
        try {ntp.getTime();}
        catch (Exception ignore) {}
        
        verify(client).connect("alfa");
        verify(client).connect("beta");
    }
}
