package com.workshare.msnos.core.protocols.ip.http;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.SingleReceipt;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoints;
import com.workshare.msnos.soup.threading.Multicaster;

public class HttpGateway implements Gateway {

    private static Logger log = LoggerFactory.getLogger(HttpGateway.class);

    private final Set<Endpoint> endpoints;
    private final HttpClient client;
    
    public HttpGateway(HttpClient client, Multicaster<Listener, Message> caster) {
        this.client = client;
        this.endpoints = new CopyOnWriteArraySet<Endpoint>();
    }
    
    @Override
    public void addListener(Cloud cloud, Listener listener) {
    }

    @Override
    public Receipt send(Cloud cloud, Message message) throws IOException {
        
//        HttpPost request = new HttpPost(urlMsgs+"?cloud=" + cloud.getIden().getUUID());
//        request.setEntity(toInputStreamEntity(messages));
//        HttpResponse res = client.execute(request);
//        EntityUtils.consume(res.getEntity());
//
        return new SingleReceipt(Status.FAILED, message);
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Endpoints endpoints() {
        return new Endpoints() {

            @Override
            public Set<Endpoint> all() {
                return endpoints;
            }

            @Override
            public Endpoint install(Endpoint endpoint) throws MsnosException {
                endpoints.add(endpoint);
                log.debug("Installed endpoint {}, all: {}",endpoint, endpoints);
                return endpoint;
            }

            @Override
            public Endpoint remove(Endpoint endpoint) throws MsnosException {
                endpoints.remove(endpoint);
                log.debug("Removed endpoint {}, all: {}",endpoint, endpoints);
                return endpoint;
            }};
    }

}
