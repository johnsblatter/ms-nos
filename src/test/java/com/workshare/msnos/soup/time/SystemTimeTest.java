package com.workshare.msnos.soup.time;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemTimeTest {
    
    private static final Logger log = LoggerFactory.getLogger(SystemTime.class);

    @Test
    public void shouldNTPTimeResembleLocalTime() throws Exception {
        long ntpTime = SystemTime.NTP_TIMESOURCE.millis();
        long sysTime = SystemTime.SYS_TIMESOURCE.millis();
        log.info("ntp: {}",ntpTime);
        log.info("sys: {}",sysTime);
        
        assertEquals(ntpTime, sysTime, 5000L);
    }
}
