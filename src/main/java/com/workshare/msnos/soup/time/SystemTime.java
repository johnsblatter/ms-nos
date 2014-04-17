package com.workshare.msnos.soup.time;


public class SystemTime {

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
        return source.millis();
    }

    public static void setTimeSource(TimeSource source) {
        SystemTime.source = source;
    }

    public static void reset() {
        setTimeSource(DEFAULT_TIMESOURCE);
    }
}
