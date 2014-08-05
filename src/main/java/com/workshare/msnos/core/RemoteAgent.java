package com.workshare.msnos.core;

import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RemoteAgent implements Agent {

    private final Iden iden;
    private final Cloud cloud;

    private AtomicLong seq;
    private Set<Network> hosts;
    private long accessTime;


    public RemoteAgent(UUID uuid, Cloud cloud, Set<Network> networks) {
        this.iden = new Iden(Iden.Type.AGT, uuid);
        this.cloud = cloud;
        this.hosts = networks;
        this.seq = new AtomicLong();
    }

    public void setSeq(long seq) {
        this.seq.set(seq);
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
        return seq.get();
    }

    @Override
    public long getAccessTime() {
        return accessTime;
    }

    @Override
    public Set<Network> getHosts() {
        return hosts;
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

    @Override
    public void touch() {
        this.accessTime = SystemTime.asMillis();
    }
}
