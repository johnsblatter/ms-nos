package com.workshare.msnos.core;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.soup.json.Json;

public class RemoteAgent extends RemoteEntity implements Agent {

    public static final Set<Endpoint> NO_ENDPOINTS = Collections.emptySet();
    
    private final Set<Endpoint> endpoints;
    private final Ring ring;

    public RemoteAgent(UUID uuid, Cloud cloud, Set<Endpoint> endpoints) {
        this(uuid, cloud, endpoints, Ring.make(endpoints));
    }

    public RemoteAgent(UUID uuid, Cloud cloud, Set<Endpoint> endpoints, Ring ring) {
        super(new Iden(Iden.Type.AGT, uuid), cloud);
        this.endpoints = toUnmodifiable(endpoints);
        this.ring = ring;
        
        touch();
    }

    private Set<Endpoint> toUnmodifiable(Set<Endpoint> endpoints) {
        if (endpoints == null)
            return NO_ENDPOINTS;
        else
            return Collections.unmodifiableSet(endpoints);
    }

    @Override
    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public Ring getRing() {
        return ring;
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

    public RemoteAgent with(Set<Endpoint> newEndpoints) {
        return new RemoteAgent(getIden().getUUID(), getCloud(), newEndpoints, getRing());
    }
}
