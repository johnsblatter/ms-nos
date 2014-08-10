package com.workshare.msnos.soup.time;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.soup.time.NTPCachedTimeSource;
import com.workshare.msnos.soup.time.NTPClient;

public class NTPCachedTimeSourceTest {

    private NTPClient ntp;
    private NTPCachedTimeSource source;

    private Long millis = null;
    private Long nanos = null;

    @Before
    public void before() {
        ntp = mock(NTPClient.class);

        source = new NTPCachedTimeSource(ntp) {
            @Override
            protected long sysMillis() {
                return millis != null ? millis : super.sysMillis();
            }

            @Override
            protected long sysNanos() {
                return nanos != null ? nanos : super.sysNanos();
            }
        };
        
    }

    @Test
    public void shouldReturnNTPTimeOnFirstTimeSuccess() throws Exception {
        when(ntp.getTime()).thenReturn(123L);
        assertEquals(123L, source.millis());
    }

    @Test
    public void shouldReturnMillisOnFirstTimeFailure() throws Exception {
        fakeSysMillis(999L);
        when(ntp.getTime()).thenThrow(new RuntimeException("boom!"));
        assertEquals(999L, source.millis());
    }

    @Test
    public void shouldNotCallNTPTimeAfterFirstTime() throws Exception {
        fakeSysNanos(1100);
        when(ntp.getTime()).thenReturn(1000L);
        assertEquals(1000L, source.millis());
        reset(ntp);
            
        fakeSysNanos(1300);
        assertEquals(1200L, source.millis());
        
        fakeSysNanos(1500);
        assertEquals(1400L, source.millis());

        verifyZeroInteractions(ntp);
    }

    private void fakeSysMillis(final long millis) {
        this.millis = millis;
    }

    private void fakeSysNanos(final long millis) {
        this.nanos = toNanos(millis);
    }

    private Long toNanos(long millis) {
        return millis * 1000l * 1000l;
    }

}
