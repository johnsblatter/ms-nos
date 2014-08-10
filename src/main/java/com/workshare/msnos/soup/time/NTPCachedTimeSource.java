package com.workshare.msnos.soup.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.soup.time.SystemTime.TimeSource;


public class NTPCachedTimeSource implements TimeSource {

    private static final Logger log = LoggerFactory.getLogger(NTPCachedTimeSource.class);

    private long ntpTime;
    private long monTime;
    
    private final NTPClient ntpClient;
    
    public NTPCachedTimeSource(NTPClient ntpClient) {
        this.ntpClient = ntpClient;
    }

    public long millis() {
        if (ntpTime != 0) {
            long delta = monoMillis() - monTime;
            return ntpTime + delta;
        }
        
        try {
            ntpTime = ntpClient.getTime();
            monTime = monoMillis();
            log.info("Succeesfully collect time from NTP sources!");
            return ntpTime;
        } catch (Exception e) {
            log.warn("Unable to successful collect time from NTP sources - will try again tough!");
            return sysMillis();
        }
    }

    private final long monoMillis() {
        return sysNanos()/1000000l;
    }
    
    protected long sysMillis() {
        return System.currentTimeMillis();
    }

    protected long sysNanos() {
        return System.nanoTime();
    }
}
