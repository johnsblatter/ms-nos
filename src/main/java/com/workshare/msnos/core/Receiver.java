package com.workshare.msnos.core;

import static com.workshare.msnos.soup.Shorteners.shorten;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.Cloud.Listener;
import com.workshare.msnos.core.cloud.MessageValidators;
import com.workshare.msnos.core.cloud.MessageValidators.Result;
import com.workshare.msnos.core.cloud.Multicaster;
import com.workshare.msnos.core.routing.Router;
import com.workshare.msnos.soup.json.Json;

public class Receiver {

    private static final Logger log = LoggerFactory.getLogger(Receiver.class);
    private static final Logger proto = LoggerFactory.getLogger("protocol");

    private final Cloud cloud;
    private final Set<Gateway> gates;
    private final Multicaster caster;
    private final MessageValidators validators;
    private final Internal internal;
    private final Router router;

    Receiver(Cloud cloud, Set<Gateway> gates, Multicaster multicaster) {
        this(cloud, gates, multicaster, new Router(cloud, gates));
    }

    Receiver(Cloud cloud, Set<Gateway> gates, Multicaster multicaster, Router router) {
        this.cloud = cloud;
        this.caster = multicaster;
        this.gates = Collections.unmodifiableSet(gates);
        this.internal = cloud.internal();
        this.validators = cloud.validators();
        this.router = router;

        for (final Gateway gate : gates) {
            gate.addListener(this.cloud, new Gateway.Listener() {
                @Override
                public void onMessage(Message message) {
                    process(message, gate.name());
                }
            });
        }
    }

    public Set<Gateway> gateways() {
        return gates;
    }

    public Multicaster caster() {
        return caster;
    }

    public Listener addListener(Listener listener) {
        log.debug("Adding listener: {}", listener);
        return caster.addListener(listener);
    }

    public void removeListener(Listener listener) {
        log.debug("Removing listener: {}", listener);
        caster.removeListener(listener);
    }

    public void process(Message message, String gateName) {
        Result result = validators.isReceivable(message);
        if (!result.success()) {
            logNN(message, gateName, result.reason());
            return;
        }

        if (isAddressedToLocal(message)) {
            logRX(message, gateName);

            message.getData().process(message, internal);
            cloud.postProcess(message);

            caster.dispatch(message);
        }

        router.forward(message);
    }

    private boolean isAddressedToLocal(Message message) {
        Iden to = message.getTo();
        return cloud.getIden().equals(to) || cloud.containsLocalAgent(to);    
    }

    private void logNN(Message msg, String gateName, String cause) {
        if (!proto.isDebugEnabled())
            return;

        final String muid = shorten(msg.getUuid());
        final String payload = Json.toJsonString(msg.getData());

        Iden from = msg.getFrom();
        if (internal.localAgents().containsKey(from))
            proto.trace("R#({}): ={}= {} {} {} {} {} {}", shorten(gateName, 3), cause, msg.getType(), muid, msg.getWhen(), msg.getFrom(), msg.getTo(), payload);
        else
            proto.debug("R#({}): ={}= {} {} {} {} {} {}", shorten(gateName, 3), cause, msg.getType(), muid, msg.getWhen(), msg.getFrom(), msg.getTo(), payload);
    }

    private void logRX(Message msg, String gateName) {
        if (!proto.isInfoEnabled())
            return;

        final String muid = shorten(msg.getUuid());
        final String payload = Json.toJsonString(msg.getData());
        proto.info("RX({}): {} {} {} {} {} {}", shorten(gateName, 3), msg.getType(), muid, msg.getWhen(), msg.getFrom(), msg.getTo(), payload);
    }
}
