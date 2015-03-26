package com.workshare.msnos.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.soup.json.Json;

public class RemoteAgent extends RemoteEntity implements Agent {

    public static final Set<Endpoint> NO_ENDPOINTS = Collections.emptySet();
    private static final Logger log = LoggerFactory.getLogger(RemoteAgent.class);
    
    private final Ring ring;
    private volatile Set<Endpoint> endpoints;

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
            return Collections.unmodifiableSet(new HashSet<Endpoint>(endpoints));
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

    public void update(Set<Endpoint> newEndpoints) {
        if (newEndpoints.size() == 0) 
            log.error("Zero endpoints received! Wtf?");
        
        this.endpoints = toUnmodifiable(newEndpoints);
        touch();
    }
}
