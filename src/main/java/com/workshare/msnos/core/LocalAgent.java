package com.workshare.msnos.core;

import com.workshare.msnos.core.Cloud.Listener;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static com.workshare.msnos.core.Message.Type.DSC;
import static com.workshare.msnos.core.Message.Type.PIN;

public class LocalAgent implements Agent {

    private static Logger log = LoggerFactory.getLogger(LocalAgent.class);

    private final Iden iden;

    private Cloud cloud;
    private Listener listener;
    private Set<Network> hosts;

    public LocalAgent(UUID uuid) {
        this.iden = new Iden(Iden.Type.AGT, uuid);
    }

    LocalAgent(Iden iden, Cloud cloud) {
        validate(iden, cloud);
        this.iden = iden;
        this.cloud = cloud;
    }

    public LocalAgent(Iden iden, Cloud cloud, Set<Network> hosts) {
        this.iden = iden;
        this.cloud = cloud;
        this.hosts = hosts;
    }

    public LocalAgent withHosts(Set<Network> hosts) {
        return new LocalAgent(iden, cloud, hosts);
    }

    @Override
    public Iden getIden() {
        return iden;
    }

    @Override
    public Cloud getCloud() {
        return cloud;
    }

    public long getAccessTime() {
        return SystemTime.asMillis();
    }

    public LocalAgent join(Cloud cloud) throws IOException {
        this.cloud = cloud;
        cloud.onJoin(this);
        log.debug("Joined: {} as Agent: {}", getCloud(), this);
        listener = cloud.addListener(new Listener() {
            @Override
            public void onMessage(Message message) {
                log.debug("Message received.");
                process(message);
            }
        });
        return this;
    }

    public void leave(Cloud cloud) throws IOException {
        log.debug("Leaving cloud {}", cloud);
        cloud.onLeave(this);
        cloud.removeListener(listener);
        log.debug("So long {}", cloud);
    }

    public Receipt send(Message message) throws IOException {
        return cloud.send(message);
    }

    private void process(Message message) {
        if (isDiscovery(message)) processDiscovery(message);
        else if (isPing(message)) processPing(message);
    }

    private void processDiscovery(Message message) {
        log.debug("Processing discovery: {}", message);
        try {
            send(Messages.presence(this, cloud));
        } catch (IOException e) {
            log.warn("Could not send message. ", e);
        }
    }

    private void processPing(Message message) {
        log.debug("Processing ping: {} ", message);
        try {
            send(Messages.pong(this, cloud));
        } catch (IOException e) {
            log.warn("Could not send message. ", e);
        }
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

    private void validate(Iden iden, Cloud cloud) {
        if (cloud == null)
            throw new IllegalArgumentException("Invalid cloud");
        if (iden == null || iden.getType() != Iden.Type.AGT)
            throw new IllegalArgumentException("Invalid iden");
    }

    private boolean isPing(Message message) {
        return message.getType() == PIN;
    }

    private boolean isDiscovery(Message message) {
        return message.getType() == DSC;
    }

    public Set<Network> getHosts() {
        return hosts;
    }
}
