package com.workshare.msnos.core.protocols.ip.www;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.serializers.WireSerializer;

public class WWWGateway implements Gateway {
    static final String SYSP_SYNC_PERIOD = "com.ws.nsnos.www.sync.period.millis";
    static final String SYSP_ADDRESS = "com.ws.nsnos.www.address";

    private static Logger log = LoggerFactory.getLogger(WWWGateway.class);
    
    private ScheduledExecutorService scheduler;
    private HttpClient client;
    private String wwwroot;

    public WWWGateway(HttpClient client, ScheduledExecutorService scheduler, WireSerializer serializer) {
        long period = loadSyncPeriod();

        this.client = client;
        this.scheduler = scheduler;
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    sync();
                } catch (IOException ex) {
                    log.warn("Unexpected exception during sync", ex);
                }
            }} , period, period, TimeUnit.MILLISECONDS);
        
        this.wwwroot = System.getProperty(SYSP_ADDRESS, "https://www.zapnos.org/api/1.0/");
    }
    
    @Override
    public void addListener(Listener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<? extends Endpoint> endpoints() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Receipt send(Message message) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    private void sync() throws IOException {
        HttpPost request = new HttpPost(newURI(wwwroot));
        client.execute(request);
    }

    private URI newURI(String path) throws IOException {
        try {
            return new URI(path);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static Long loadSyncPeriod() {
        return Long.getLong(SYSP_SYNC_PERIOD, 5000L);
    }


}
