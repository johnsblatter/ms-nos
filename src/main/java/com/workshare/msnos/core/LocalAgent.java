package com.workshare.msnos.core;

import com.workshare.msnos.core.Cloud.Listener;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.workshare.msnos.core.Message.Type.DSC;
import static com.workshare.msnos.core.Message.Type.PIN;

public class LocalAgent implements Agent {

    private static Logger log = LoggerFactory.getLogger(LocalAgent.class);

    private final Iden iden;
    private final AtomicLong seq;

    private Cloud cloud;
    private Listener listener;
    private Set<Network> hosts;

    public LocalAgent(UUID uuid) {
        Iden iden = new Iden(Iden.Type.AGT, uuid);
        validate(iden);

        this.iden = iden;
        this.seq = new AtomicLong(SystemTime.asMillis());
    }

    public void setHosts(Set<Network> hosts) {
        this.hosts = hosts;
    }

    @Override
    public Set<Network> getHosts() {
        return hosts;
    }

    @Override
    public Iden getIden() {
        return iden;
    }

    @Override
    public Cloud getCloud() {
        return cloud;
    }

    @Override
    public Long getSeq() {
        return seq.incrementAndGet();
    }

    @Override
    public void touch() {
    }

    @Override
    public long getAccessTime() {
        return SystemTime.asMillis();
    }

    public LocalAgent join(Cloud cloud) throws MsnosException {
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
        setHosts(new Presence(true).getNetworks());
        return this;
    }

    public void leave() throws MsnosException {
        if (this.cloud == null) {
            throw new MsnosException("Cannot leave a cloud I never joined!", MsnosException.Code.INVALID_STATE);
        }

        log.debug("Leaving cloud {}", cloud);
        cloud.onLeave(this);
        cloud.removeListener(listener);
        log.debug("So long {}", cloud);
    }

    public Receipt send(Message message) throws MsnosException {
        return cloud.send(message);
    }

    private void validate(Iden iden) {
        if (iden == null || iden.getType() != Iden.Type.AGT) throw new IllegalArgumentException("Invalid iden");
    }

    private void process(Message message) {
        if (isDiscovery(message)) processDiscovery(message);
        else if (isPing(message)) processPing(message);
    }

    private boolean isPing(Message message) {
        return message.getType() == PIN;
    }

    private boolean isDiscovery(Message message) {
        return message.getType() == DSC;
    }

    private void processDiscovery(Message message) {
        log.debug("Processing discovery: {}", message);
        try {
            send(new MessageBuilder(Message.Type.PRS, this, cloud).with(new Presence(true)).make());
        } catch (MsnosException e) {
            log.warn("Could not send message. ", e);
        }
    }

    private void processPing(Message message) {
        log.debug("Processing ping: {} ", message);
        try {
            send(new MessageBuilder(Message.Type.PON, this, cloud).make());
        } catch (MsnosException e) {
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
}
