package com.workshare.msnos.core.protocols.ip.http;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Iden.Type;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.SingleReceipt;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoints;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.serializers.WireJsonSerializer;

public class HttpGateway implements Gateway {

    private static Logger log = LoggerFactory.getLogger(HttpGateway.class);

    private final Map<Iden, HttpEndpoint> endpoints;
    private final HttpClient client;
    private final WireJsonSerializer serializer;
    
    public HttpGateway(HttpClient client) {
        this.client = client;
        this.endpoints = new ConcurrentHashMap<Iden, HttpEndpoint>();
        this.serializer = new WireJsonSerializer();
    }

    @Override
    public String name() {
        return "HTTP";
    }

    @Override
    public void addListener(Cloud cloud, Listener listener) {
    }

    @Override
    public Receipt send(Cloud cloud, Message message) throws IOException {
        if (message.getTo().getType() == Type.CLD) {
            for (HttpEndpoint endpoint : endpoints.values()) {
                RemoteAgent remote = cloud.find(endpoint.getTarget());
                if (remote != null && !cloud.getRing().equals(remote.getRing()))
                    sendTo(message, endpoint);
            }
            
            return new SingleReceipt(this, Status.PENDING, message);
        }
        else {
            HttpEndpoint endpoint = endpoints.get(message.getTo());
            if (endpoint == null)
                return new SingleReceipt(this, Status.FAILED, message);
    
            return sendTo(message, endpoint);
        }
    }

    private Receipt sendTo(Message message, HttpEndpoint endpoint) {
        try {
            HttpPost request = new HttpPost(endpoint.getUrl());
            request.setEntity(new StringEntity(serializer.toText(message)));
            HttpResponse res = client.execute(request);
            consume(res);
            return new SingleReceipt(this, Status.DELIVERED, message);
        }
        catch (IOException ex) {
            if (log.isDebugEnabled())
                log.debug("Unexpected exception sending message "+message+" to url "+endpoint.getUrl(), ex);
            else
                log.warn("Unexpected exception sending message "+message+" to url "+endpoint.getUrl());
            
            return new SingleReceipt(this, Status.FAILED, message);
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Endpoints endpoints() {
        return new Endpoints() {

            @Override
            public Set<Endpoint> all() {
                return new HashSet<Endpoint>(endpoints.values());
            }

            @Override
            public Endpoint install(Endpoint endpoint) throws MsnosException {
                HttpEndpoint httpEndpoint = ensureHttp(endpoint);
                endpoints.put(httpEndpoint.getTarget(), httpEndpoint);
                log.debug("Installed endpoint {}, all: {}",endpoint, endpoints);
                return endpoint;
            }

            @Override
            public Endpoint remove(Endpoint endpoint) throws MsnosException {
                endpoints.remove(ensureHttp(endpoint).getTarget());
                log.debug("Removed endpoint {}, all: {}",endpoint, endpoints);
                return endpoint;
            }

            private HttpEndpoint ensureHttp(Endpoint endpoint) throws MsnosException {
                if (!(endpoint instanceof HttpEndpoint))
                    throw new MsnosException("The HTTP gateway accepts only HTTP endpoints", MsnosException.Code.UNRECOVERABLE_FAILURE);
                HttpEndpoint ep = (HttpEndpoint) endpoint;
                if (ep.getTarget().equals(Iden.NULL))
                    throw new MsnosException("The HTTP gateway accepts only targeted endpoints", MsnosException.Code.UNRECOVERABLE_FAILURE);
                return ep;
            }

            @Override
            public Set<? extends Endpoint> of(Agent agent) {
                HashSet<Endpoint> result = new HashSet<Endpoint>(endpoints.size());
                for(HttpEndpoint endpoint : endpoints.values()) {
                    if (agent.getIden().equals(endpoint.getTarget()))
                        result.add(endpoint);
                }
                return result;
            }

            @Override
            public Set<? extends Endpoint> publics() {
                HashSet<Endpoint> result = new HashSet<Endpoint>(endpoints.size());
                for(HttpEndpoint endpoint : endpoints.values()) {
                    if (endpoint.getTarget() == Iden.NULL)
                        result.add(endpoint);
                }
                return result;
            }};
    }

    private void consume(HttpResponse res)  {
        EntityUtils.consumeQuietly(res.getEntity());
    }
}
