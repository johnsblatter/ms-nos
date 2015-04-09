package com.workshare.msnos.core.routing;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpiringMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Identifiable;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.NoopGateway;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.www.WWWGateway;

public class Router {

    public static final Gateway NOOP_GATE = new NoopGateway();

    public static final String SYSP_PROCESS_EXPIRE = "com.ws.nsnos.core.router.expire.millis";
    public static final String SYSP_MAXIMUM_HOPS_CLOUD = "com.ws.nsnos.core.router.hops.cloud.max";
    public static final String SYSP_MAXIMUM_HOPS_DIRECT = "com.ws.nsnos.core.router.hops.direct.max";
    public static final String SYSP_MAXIMUM_MESSAGES_PER_RING = "com.ws.nsnos.core.router.ring,messages.max";

    private static final Logger routing = LoggerFactory.getLogger("routing");
    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private static final long ONE_SECOND = 1000l;

    private final Cloud cloud;
    private final Gateway udpGate;
    private final Gateway wwwGate;
    private final Gateway httpGate;
    private final Map<UUID, Message> processed;
    
    private final Route[] routes;

    public Router(Cloud cloud, Set<Gateway> gates) {
        this(cloud, getGate(gates, UDPGateway.class), getGate(gates, HttpGateway.class), getGate(gates, WWWGateway.class));
    }

    Router(Cloud cloud, UDPGateway udpGate, HttpGateway httpGate, WWWGateway wwwGate) {
        this.cloud = cloud;
        this.udpGate = (udpGate != null ? udpGate : NOOP_GATE);
        this.wwwGate = (wwwGate != null ? wwwGate : NOOP_GATE);
        this.httpGate = (httpGate != null ? httpGate : NOOP_GATE);
        
        long expirytime = Long.getLong(SYSP_PROCESS_EXPIRE, 30*ONE_SECOND);
        this.processed = ExpiringMap.builder().expiration(expirytime, TimeUnit.MILLISECONDS).build();
        
        this.routes = new Route[] {
            new FailingRouteOnZeroHops(this),
            new FailingRouteOnMessageSeen(this),
            new WWWRouteBroadcast(this),
            new CloudRouteBroadcast(this),
            new HTTPRouteDirect(this),
            new UDPRouteSameRing(this),
            new HTTPRouteViaRing(this),
            new UDPRouteBroadcast(this),
        };
    }

    public Gateway udpGateway() {
        return udpGate;
    }
    
    public Gateway httpGateway() {
        return httpGate;
    }
    
    public Gateway wwwGateway() {
        return wwwGate;
    }
    
    public Receipt process(Message message) throws IOException {
        logger.debug("Routing message {}", message);
 
        for (Route route : routes) {
            Receipt receipt = route.send(message);
            if (receipt != null) {
                if (logger.isDebugEnabled())
                    logger.debug("Message {} routed via {}, result is {}", message, route.getClass().getSimpleName(), receipt);

                if (receipt.getStatus() != Status.FAILED)
                    processed.put(message.getUuid(), message);

                return receipt;
            }
        }
        
        throw new IOException("Unable to find a suitable way to route the message!");
    }

    Cloud cloud() {
        return cloud;
    }
 
    boolean hasRouteFor(RemoteAgent remote) {
        Set<Endpoint> endpoints = remote.getEndpoints(Type.HTTP);
        return endpoints.size() > 0;
    }

    boolean wasSeen(Message message) {
        return processed.containsKey(message.getUuid());
    }

    Receipt sendViaWWW(Message message, String how) throws IOException {
        return this.send(message, null, message.getHops(), wwwGate, how);
    }
    Receipt sendViaUDP(Message message, int hops, String how) throws IOException {
        return this.send(message, null, hops, udpGate, how);
    }

    Receipt sendViaHTTP(Message message, Identifiable to, int hops, String how) throws IOException {
        return this.send(message, to, hops, httpGate, how);
    }

    private Receipt send(Message message, Identifiable to, int hops, Gateway gate, String how) throws IOException {
        Receipt receipt = gate.send(cloud, message.withHops(hops), to);
        routing.info("RR {} {} {} {} {} {}", how, gate.name(), hops, receipt.getStatus(), message);
        return receipt;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T getGate(Set<? super T> gates, Class<T> clazz) {
        for (Object gate : gates) {
            if (gate.getClass() == clazz)
                return (T)gate;
        }
        
        return null;
    }
}
