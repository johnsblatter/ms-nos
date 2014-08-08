package com.workshare.msnos.core.cloud;

import org.apache.commons.net.time.TimeTCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimeClient {

    private static final Logger log = LoggerFactory.getLogger(TimeClient.class);

    private final List<String> serverlist;

    public TimeClient() {
        this.serverlist = new ArrayList<String>(Arrays.asList("time.nist.gov", "de.pool.ntp.org", "uk.pool.ntp.org"));
    }

    public TimeClient(List<String> serverlist) {
        this.serverlist = serverlist;
    }

    public Long getTime() throws IOException {
        TimeTCPClient client = new TimeTCPClient();
        client.setDefaultTimeout(4000);

        for (String hostname : serverlist) {
            client.connect(hostname);
            try {
                if (client.isConnected()) {
                    Long time = client.getTime();
                    log.debug("Time is: {}", time);
                    return time;
                }
            } catch (IOException e) {
                log.error("Server: {} not responding, possibly due to another request being made to this server from this IP in the last 4 seconds.", hostname);
            } finally {
                try {
                    client.disconnect();
                } catch (Throwable ex) {
                    log.error("Unable to disconnect time client: {}", ex);
                }
            }
        }

        throw new IOException("Unable to connect to time servers.");
    }
}

