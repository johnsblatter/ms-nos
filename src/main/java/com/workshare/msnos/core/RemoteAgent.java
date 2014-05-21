package com.workshare.msnos.core;

import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;

import java.util.Set;
import java.util.UUID;

public class RemoteAgent implements Agent {

    private final Iden iden;
    private final Cloud cloud;

    private Set<Network> hosts;
    private long accessTime;

    public RemoteAgent(UUID uuid, Cloud cloud) {
        this.iden = new Iden(Iden.Type.AGT, uuid);
        this.cloud = cloud;
    }

    public RemoteAgent withHosts(Set<Network> networks) {
        final RemoteAgent remoteAgent = new RemoteAgent(iden.getUUID(), cloud);
        remoteAgent.hosts = networks;
        return remoteAgent;
    }

    @Override
    public Iden getIden() {
        return iden;
    }

    public Cloud getCloud() {
        return cloud;
    }

    @Override
    public long getAccessTime() {
        return accessTime;
    }

    @Override
    public Set<Network> getHosts() {
        return hosts;
    }

    public void touch() {
        setAccessTime(SystemTime.asMillis());
    }

    private void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
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
