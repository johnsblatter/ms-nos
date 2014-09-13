package com.workshare.msnos.core;

import java.util.Set;
import java.util.UUID;

import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.soup.json.Json;

public class RemoteAgent extends RemoteEntity implements Agent {

    private Set<Endpoint> endpoints;
    public RemoteAgent(UUID uuid, Cloud cloud, Set<Endpoint> endpoints) {
        super(new Iden(Iden.Type.AGT, uuid), cloud);
        this.endpoints = endpoints;
    }

    @Override
    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
    public boolean equals(Object other) {
        try {
            return this.getIden().equals(((Agent) (other)).getIden());
        } catch (Exception any) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getIden().hashCode();
    }
}
