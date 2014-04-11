package com.workshare.msnos.core;

import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.soup.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;

public class Agent implements Identifiable {

    private static Logger log = LoggerFactory.getLogger(Agent.class);

    // FIXME this cannot be hardcoded!!!
    public static final int DEFAULT_HOPS = 3;
    private final Iden iden;
    private transient Cloud cloud;

    public Agent(UUID uuid) {
        this.iden = new Iden(Iden.Type.AGT, uuid);
    }

    Agent(Iden iden, Cloud cloud) {
        validate(iden, cloud);
        this.iden = iden;
        this.cloud = cloud;
    }

    private void validate(Iden iden, Cloud cloud) {
        if (cloud == null)
            throw new IllegalArgumentException("Invalid cloud");
        if (iden == null || iden.getType() != Iden.Type.AGT)
            throw new IllegalArgumentException("Invalid iden");
    }

    public Iden getIden() {
        return iden;
    }

    public Agent join(Cloud cloud) throws IOException {
        this.cloud = cloud;
        cloud.onJoin(this);
        log.debug("Joined: " + getCloud() + " as Agent: " + this);
        cloud.addListener(new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                log.debug("Message received.");
                process(message);
            }
        });
        return this;
    }

    public void leave(Cloud cloud) throws IOException {
        log.debug("Leaving cloud " + cloud);
        cloud.onLeave(this);
        sendMessage(Messages.absence(this, cloud));
        log.debug("So long " + cloud);
    }

    private void process(Message message) {
        if (message.getType() == Type.DSC) processDiscovery(message);
    }

    private void processDiscovery(Message message) {
        log.debug("Processing discovery: " + message);
        try {
            sendMessage(Messages.presence(this, cloud));
        } catch (IOException e) {
            log.debug("Could not send message. ", e);
        }
    }

    public Cloud getCloud() {
        return cloud;
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
    public boolean equals(Object other) {
        try {
            return this.iden.equals(((Agent) (other)).getIden());
        } catch (Exception any) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return iden.hashCode();
    }

    public Future<Status> sendMessage(Message message) throws IOException {
        return cloud.send(message);
    }

    public Future<Status> sendReliableMessage(Message message) throws IOException {
        return cloud.send(message);
    }
}
