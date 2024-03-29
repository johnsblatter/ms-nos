package com.workshare.msnos.soup.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SystemTime {

    private static final Logger log = LoggerFactory.getLogger(SystemTime.class);

    public interface TimeSource {
        long millis();
        void sleep(long millis) throws InterruptedException;
    }

    public static final TimeSource NTP_TIMESOURCE = new NTPCachedTimeSource(new NTPClient());
    public static final TimeSource SYS_TIMESOURCE = new TimeSource(){
        @Override
        public long millis() {
            return System.currentTimeMillis();
        }

        @Override
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }};

    private static final TimeSource DEFAULT_TIMESOURCE;
    static {
        if (Boolean.getBoolean("com.ws.nsnos.time.local")) {
            DEFAULT_TIMESOURCE = SYS_TIMESOURCE;
            log.warn("Using local system timesource");
        }
        else {
            DEFAULT_TIMESOURCE = NTP_TIMESOURCE;
            log.info("Using NTP powered timesource");
        }
        
        // boot
        DEFAULT_TIMESOURCE.millis();
    }

    private static TimeSource source = DEFAULT_TIMESOURCE;

    public static void sleep(long millis) throws InterruptedException {
        source.sleep(millis);
    }

    public static long asMillis() {
        return source.millis();
    }

    public static void setTimeSource(TimeSource source) {
        SystemTime.source = source;
    }

    public static void reset() {
        setTimeSource(DEFAULT_TIMESOURCE);
    }
}
