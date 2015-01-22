package com.workshare.msnos.core.protocols.ip.www;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.SingleReceipt;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoints;
import com.workshare.msnos.core.serializers.WireSerializer;
import com.workshare.msnos.soup.threading.ConcurrentBuildingMap;
import com.workshare.msnos.soup.threading.ConcurrentBuildingMap.Factory;
import com.workshare.msnos.soup.threading.Multicaster;

public class WWWGateway implements Gateway {

    public static final String SYSP_SYNC_PERIOD = "com.ws.nsnos.www.sync.period.millis";
    public static final String SYSP_ADDRESS = "com.ws.nsnos.www.address";

    private static final UUID NULL = new UUID(0000,000);

    private static Logger log = LoggerFactory.getLogger(WWWGateway.class);

    private final ScheduledExecutorService scheduler;
    private final HttpClient client;
    private final WireSerializer serializer;
    private final Map<Cloud, UUID> cloudListeners;
    private final Map<Cloud, Queue<Message>> cloudMessages;
    private final Multicaster<Listener, Message> caster;

    private final String urlRoot;
    private final String urlMsgs;

    private AtomicInteger syncing = new AtomicInteger(0);

    public WWWGateway(HttpClient client, ScheduledExecutorService scheduler, WireSerializer serializer,
            Multicaster<Listener, Message> caster) throws IOException {
        
        this.client = client;
        this.caster = caster;
        this.scheduler = scheduler;
        this.serializer = serializer;
        this.cloudListeners = new ConcurrentHashMap<Cloud, UUID>();
        this.cloudMessages = new ConcurrentBuildingMap<Cloud, Queue<Message>>(new Factory<Queue<Message>>() {
            @Override
            public Queue<Message> make() {
                return new ConcurrentLinkedQueue<Message>();
            }
        });

        long period = loadSyncPeriod();
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sync();
            }
        }, period, period, TimeUnit.MILLISECONDS);

        this.urlRoot = System.getProperty(SYSP_ADDRESS, "https://www.zapnos.org/");
        this.urlMsgs = composeUrl("api/1.0/messages");

        ping(client);
    }

    @Override
    public String name() {
        return "WWW";
    }

    private void ping(HttpClient client) throws IOException, ClientProtocolException, MalformedURLException {
        HttpResponse response = client.execute(new HttpGet(composeUrl("ping")));
        EntityUtils.consume(response.getEntity());
    }

    private String composeUrl(String path) throws MalformedURLException {
        return new URL(new URL(urlRoot), path).toExternalForm();
    }

    @Override
    public void close() throws IOException {
        // TODO please fixme, I am unimplemented :)
    }

    @Override
    public void addListener(Cloud cloud, Listener listener) {
        cloudListeners.put(cloud, NULL);
        caster.addListener(listener);
    }

    @Override
    public Endpoints endpoints() {
        return BaseEndpoint.create();
    }

    @Override
    public Receipt send(Cloud cloud, Message message) throws IOException {
        cloudMessages.get(cloud).add(message);
        return new SingleReceipt(this, Status.PENDING, message);
    }

    private void sync() {
        int value = syncing.incrementAndGet();
        try {
            if (value > 1) {
                log.warn("Request to sync while syncing was in progress");
                return;
            } else {
                doSync();
            }
        } finally {
            syncing.decrementAndGet();
        }
    }

    private void doSync() {
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
        Set<Cloud> clouds = new HashSet<Cloud>(cloudListeners.keySet());
        for (Cloud cloud : clouds) {
            String url = urlMsgs+"?cloud=" + cloud.getIden().getUUID();
            UUID message = cloudListeners.get(cloud);
            if (message != NULL)
                url += "&message=" + message;
                
            HttpGet request = new HttpGet(url);
            HttpResponse res = client.execute(request);
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(res.getEntity().getContent(), "UTF-8"));
                try {
                    String line;
                    Message last = null;
                    while((line=in.readLine())!=null) {
                        Message msg = serializer.fromText(line, Message.class);
                        if (msg != null) {
                            caster.dispatch(msg);
                            last = msg;
                        }
                    }
                    
                    if (last != null)
                        cloudListeners.put(cloud, last.getUuid());
                } finally {
                    in.close();
                }
            } finally {
                EntityUtils.consume(res.getEntity());
            }
        }
    }

    private void syncTx() throws IOException {
        if (cloudMessages.size() == 0) {
            log.debug("No messages to send so far");
            return;
        }

        Set<Cloud> clouds = new HashSet<Cloud>(cloudMessages.keySet());
        for (Cloud cloud : clouds) {
            Queue<Message> messages = cloudMessages.get(cloud);
            if (messages.size() == 0)
                continue;

            HttpPost request = new HttpPost(urlMsgs+"?cloud=" + cloud.getIden().getUUID());
            request.setEntity(toInputStreamEntity(messages));
            HttpResponse res = client.execute(request);
            EntityUtils.consume(res.getEntity());
        }
    }

    private InputStreamEntity toInputStreamEntity(final Queue<Message> messages) {
        return new InputStreamEntity(new MessagesInputSream(serializer, messages), ContentType.TEXT_PLAIN);
    }

    private static Long loadSyncPeriod() {
        return Long.getLong(SYSP_SYNC_PERIOD, 5000L);
    }
}
