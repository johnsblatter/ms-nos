package com.workshare.msnos.soup.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemTime {

    private static final Logger log = LoggerFactory.getLogger(SystemTime.class);

    public interface TimeSource {
        long millis();
    }

    private static final TimeSource DEFAULT_TIMESOURCE =
            new TimeSource() {
                public long millis() {
                    return System.currentTimeMillis();
                }
            };

    private static TimeSource source = DEFAULT_TIMESOURCE;

    public static long asMillis() {
        log.debug("System time is: {}", source.millis());
        return source.millis();
    }

    public static void setTimeSource(TimeSource source) {
        SystemTime.source = source;
    }

    public static void reset() {
        setTimeSource(DEFAULT_TIMESOURCE);
    }
}
