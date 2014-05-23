package com.workshare.msnos.core.protocols.ip.www;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.serializers.WireSerializer;

public class WWWGateway implements Gateway {
    
    public class MessagesInputSream extends InputStream {
        
        private List<Message> messages;
        private String current;
        private int messIndex;
        private int charIndex;
        
        public MessagesInputSream(List<Message> messages) {
            this.messages = messages;
            loadNextMessage();
        }

        private boolean loadNextMessage() {
            if (messIndex == messages.size())
                return false;
            
            Message message = messages.get(messIndex++);
            current = serializer.toText(message);
            charIndex = 0;
            return true;
        }

        @Override
        public int read() throws IOException {
            if (messages == null)
                return -1;
            else if (charIndex == current.length()) {
                if (!loadNextMessage()) 
                    messages = null;
                
                return (int) '\n';
            } 
            else
                return (int)current.charAt(charIndex++);
        }
    }

    static final String SYSP_SYNC_PERIOD = "com.ws.nsnos.www.sync.period.millis";
    static final String SYSP_ADDRESS = "com.ws.nsnos.www.address";

    private static Logger log = LoggerFactory.getLogger(WWWGateway.class);
    
    private final ScheduledExecutorService scheduler;
    private final HttpClient client;
    private final String wwwroot;
    private final WireSerializer serializer;

    private transient List<Message> messages;

    public WWWGateway(HttpClient client, ScheduledExecutorService scheduler, WireSerializer serializer) {
        this.client = client;
        this.scheduler = scheduler;
        this.serializer = serializer;
        
        long period = loadSyncPeriod();
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sync();
            }} , period, period, TimeUnit.MILLISECONDS);
        
        this.wwwroot = System.getProperty(SYSP_ADDRESS, "https://www.zapnos.org/api/1.0/");
        swapMessages();
    }
    
    private List<Message> swapMessages() {
        List<Message> current = this.messages;
        this.messages = new ArrayList<Message>();
        return current;
    }

    @Override
    public void addListener(Cloud cloud, Listener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<? extends Endpoint> endpoints() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Receipt send(Message message) throws IOException {
        this.messages.add(message);
        return null;
    }

    private void sync() {
        try {
            syncTx();
        } catch (Exception ex) {
            log.warn("Unexpected exception during sync (TX)", ex);
        }
        
        try {
            syncRx();
        } catch (Exception ex) {
            log.warn("Unexpected exception during sync (RX)", ex);
        }
    }

    private void syncRx() throws IOException {
        HttpGet request = new HttpGet(newURI(wwwroot));
        client.execute(request);
    }

    private void syncTx() throws IOException {
        if (messages.size() == 0) {
            log.debug("No messages to send so far");
            return;
        }
        
        HttpPost request = new HttpPost(newURI(wwwroot));
        request.setEntity(toInputStreamEntity(swapMessages()));
        HttpResponse res = client.execute(request);
        EntityUtils.consume(res.getEntity());
    }

    private InputStreamEntity toInputStreamEntity(final List<Message> messages) {
        return new InputStreamEntity(new MessagesInputSream(messages), ContentType.TEXT_PLAIN);
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
