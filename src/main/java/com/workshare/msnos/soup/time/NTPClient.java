package com.workshare.msnos.soup.time;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.net.time.TimeTCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NTPClient {

    private static final Logger log = LoggerFactory.getLogger(NTPClient.class);

    private final List<String> serverlist;
    private final TimeTCPClient client;

    public NTPClient() {
        this(defaultServerList(), defaultTimeTCPClient());
    }

    public NTPClient(List<String> serverlist) {
        this(serverlist, defaultTimeTCPClient());
    }

    public NTPClient(List<String> serverlist, TimeTCPClient client) {
        this.serverlist = serverlist;
        this.client = client;
    }

    public Long getTime() throws IOException {

        int tot = serverlist.size();
        int cur = (int) Math.random()*tot;
        int idx = 0;
        
        while(idx++ < tot) {
            String hostname = serverlist.get((idx + cur)%tot);
            log.debug("Connecting to host {}", hostname);
            
            try {
                client.connect(hostname);
                try {
                    if (client.isConnected()) {
                        Long time = client.getDate().getTime();
                        log.debug("Time is: {}", time);
                        return time;
                    }
                } finally {
                    try {
                        client.disconnect();
                    } catch (Throwable ignore) {
                    }
                }
            } catch (IOException e) {
                log.warn("Server: {} not responding, possibly due to another request being made to this server from this IP in the last 4 seconds.", hostname);
            }
        }

        throw new IOException("Unable to connect to time servers.");
    }

    private static ArrayList<String> defaultServerList() {
        return new ArrayList<String>(Arrays.asList("time.nist.gov", "ntp1.inrim.it", "ntp1.ien.it"));
    }

    private static TimeTCPClient defaultTimeTCPClient() {
        TimeTCPClient client = new TimeTCPClient();
        client.setConnectTimeout(Integer.getInteger("com.ws.nsnos.time.ntp.timeout.connect.millis", 5000));
        client.setDefaultTimeout(Integer.getInteger("com.ws.nsnos.time.ntp.timeout.default.millis", 10000));
        return client;
    }

}
