package com.workshare.msnos.core;

import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;

import java.util.Set;
import java.util.UUID;

public class RemoteAgent implements Agent {

    private final Iden iden;
    private final Cloud cloud;

    private long seq;
    private Set<Network> hosts;
    private long accessTime;


    public RemoteAgent(UUID uuid, Cloud cloud, Set<Network> networks) {
        this.iden = new Iden(Iden.Type.AGT, uuid);
        this.cloud = cloud;
        this.hosts = networks;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    @Override
    public Iden getIden() {
        return iden;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public long getSeq() {
        return seq;
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
