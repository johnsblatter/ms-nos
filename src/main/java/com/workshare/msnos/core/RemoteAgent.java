package com.workshare.msnos.core;

import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;

import java.util.Set;
import java.util.UUID;

public class RemoteAgent implements AgentInterface {

    private Iden iden;
    private Cloud cloud;
    private long accessTime;
    private Set<Network> hosts;

    public RemoteAgent(UUID uuid) {
        this.iden = new Iden(Iden.Type.AGT, uuid);
    }

    public RemoteAgent(Iden iden, Cloud cloud) {
        this.iden = iden;
        this.cloud = cloud;
    }

    public RemoteAgent(Iden iden, Cloud cloud, Set<Network> hosts) {
        this.iden = iden;
        this.cloud = cloud;
        this.hosts = hosts;
    }

    public RemoteAgent withHosts(Set<Network> networks) {
        return new RemoteAgent(iden, cloud, networks);
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
            return this.iden.equals(((AgentInterface) (other)).getIden());
        } catch (Exception any) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return iden.hashCode();
    }

}
