package com.workshare.msnos.core.routing;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Identifiable;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.core.cloud.LocalAgent;
import com.workshare.msnos.core.cloud.MessageValidators;
import com.workshare.msnos.core.cloud.MessageValidators.Result;
import com.workshare.msnos.core.payloads.TracePayload;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.protocols.ip.NullGateway;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.www.WWWGateway;
import com.workshare.msnos.core.receipts.SingleReceipt;

public class Router {

    public static final Gateway NOOP_GATE = new NullGateway();

    public static final String SYSP_MAXIMUM_HOPS_CLOUD = "com.ws.nsnos.core.router.hops.cloud.max";
    public static final String SYSP_MAXIMUM_HOPS_DIRECT = "com.ws.nsnos.core.router.hops.direct.max";
    public static final String SYSP_MAXIMUM_MESSAGES_PER_RING = "com.ws.nsnos.core.router.ring,messages.max";

    private static final Logger routing = LoggerFactory.getLogger("routing");
    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private final Cloud cloud;
    private final Gateway udpGate;
    private final Gateway wwwGate;
    private final Gateway httpGate;
    private final MessageValidators validators;
    
    private final Route[] routes;

    private static enum Mode {TXX, FWD};
    private final ThreadLocal<Mode> mode = new ThreadLocal<Router.Mode>();

    
    public Router(Cloud cloud, Set<Gateway> gates) {
        this(cloud, getGate(gates, UDPGateway.class), getGate(gates, HttpGateway.class), getGate(gates, WWWGateway.class));
    }

    Router(Cloud cloud, UDPGateway udpGate, HttpGateway httpGate, WWWGateway wwwGate) {
        this.cloud = cloud;
        this.udpGate = (udpGate != null ? udpGate : NOOP_GATE);
        this.wwwGate = (wwwGate != null ? wwwGate : NOOP_GATE);
        this.httpGate = (httpGate != null ? httpGate : NOOP_GATE);
        this.validators = cloud.validators();
        
        this.routes = new Route[] {
            new TerminalRouteOnZeroHops(this),
            new WWWRouteBroadcast(this),
            new CloudRouteBroadcast(this),
            new HTTPRouteDirect(this),
            new UDPRouteSameRing(this),
            new HTTPRouteViaRing(this),
            new UDPRouteBroadcast(this),
        };
    }

    Router(Cloud cloud, UDPGateway udpGate, HttpGateway httpGate, WWWGateway wwwGate, Route[] routes) {
        this.cloud = cloud;
        this.udpGate = (udpGate != null ? udpGate : NOOP_GATE);
        this.wwwGate = (wwwGate != null ? wwwGate : NOOP_GATE);
        this.httpGate = (httpGate != null ? httpGate : NOOP_GATE);
        this.validators = cloud.validators();
        this.routes = routes;
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
   
    public Receipt send(Message message) {
        mode.set(Mode.TXX);
        return route(message);
    }

    public Receipt forward(Message message)  {
        mode.set(Mode.FWD);

        final Result result = validators.isForwardable(message);
        if (result.success()) {
            return route(message);
        } else
            return skip(message, result.reason());
    }

    Receipt route(Message message) {
        logger.debug("Routing message {}", message);
 
        for (Route route : routes) {
            Receipt receipt = route.send(message);
            if (receipt != null) {
                if (logger.isDebugEnabled())
                    logger.debug("Message {} routed via {}, result is {}", message, route.getClass().getSimpleName(), receipt);

                return receipt;
            }
        }
        
        routing.info("{} {} {} {} {} {}", "N/A", "N/A", "NO-ROUTE", mode.get(), message);
        logger.warn("Unable to send message {} no route found", message);
        return SingleReceipt.failure(message);
    }

    Cloud cloud() {
        return cloud;
    }
 
    boolean hasRouteFor(RemoteAgent remote) {
        Set<Endpoint> endpoints = remote.getEndpoints(Type.HTTP);
        return endpoints.size() > 0;
    }

    Receipt sendViaWWW(Message message, String how)  {
        return this.send(message, null, message.getHops(), wwwGate, how);
    }
    
    Receipt sendViaUDP(Message message, int hops, String how)  {
        if (udpGate.name().equals(message.getReceivingGate())) {
            routing.info("{} {} {} {} {} {}", mode.get(), how, udpGate.name(), "UDP-TO-UDP", message);
            return SingleReceipt.failure(message);
        }
        
        return this.send(message, null, hops, udpGate, how);
    }

    Receipt sendViaHTTP(Message message, Identifiable to, int hops, String how)  {
        return this.send(message, to, hops, httpGate, how);
    }

    private Receipt send(Message message, Identifiable to, int hops, Gateway gate, String how) {
        
        if (message.getType() == Message.Type.TRC) {
            TracePayload payload = (TracePayload) message.getData();
            payload = payload.crumbed(findSource(), findDestination(to, message), gate, message.getHops());
            message = message.data(payload);
        }
        
        Receipt receipt;
        try {
            final Message hoppedMessage = message.withHops(hops);
            receipt = gate.send(cloud, hoppedMessage, to);
            routing.info("{} {} {} {} {} {} {}", mode.get(), how, gate.name(), receipt.getStatus(), hops, hoppedMessage);
        } catch (IOException e) {
            receipt = SingleReceipt.failure(message);
            routing.info("{} {} {} {} {} {}", mode.get(), how, gate.name(), "GATE-FAILURE", message);
            logger.warn("Unable to send message {} trough gateway {}", message, gate);
        }
        
        return receipt;
    }

    private UUID findDestination(Identifiable to, Message message) {
        if (to != null)
            return to.getIden().getUUID();
        else
            return message.getTo().getUUID();
    }

    private UUID findSource() {
        Collection<LocalAgent> locals = cloud.getLocalAgents();
        if (!locals.isEmpty()) 
            return locals.iterator().next().getIden().getUUID();
        else
            return cloud.getRing().uuid();
    }

    Receipt skip(Message message, String how) {
        routing.debug("{} {} {}", mode.get(), how, message);
        return SingleReceipt.failure(message);
    }
    
    Receipt terminal(Message message, String how) {
        routing.info("{} {} {} {}", mode.get(), how, "NONE", message);
        return SingleReceipt.failure(message);
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
