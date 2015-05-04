package com.workshare.msnos.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.concurrent.GuardedBy;

import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.soup.json.Json;

public class RemoteAgent extends RemoteEntity implements Agent {

    public static final Set<Endpoint> NO_ENDPOINTS = Collections.emptySet();

    private final Ring ring;

    @GuardedBy("this")
    private Set<Endpoint> endpoints;
    @GuardedBy("this") 
    private transient Map<Type, Set<Endpoint>> endpointsByType;

    public RemoteAgent(UUID uuid, Cloud cloud, Set<Endpoint> endpoints) {
        this(uuid, cloud, endpoints, Ring.make(endpoints));
    }

    private RemoteAgent(UUID uuid, Cloud cloud, Set<Endpoint> endpoints, Ring ring) {
        super(new Iden(Iden.Type.AGT, uuid), cloud);
        this.ring = ring;
        update(endpoints);
    }

    @Override
    public synchronized Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    public synchronized Set<Endpoint> getEndpoints(Type type) {
        return endpointsByType.get(type);
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
        touch();

        synchronized(this) {
            this.endpoints = createImmutableSet(newEndpoints);
            this.endpointsByType = createImmutableByTypeMap(endpoints);
        }
    }

    private Map<Type, Set<Endpoint>> createImmutableByTypeMap(Set<Endpoint> newEndpoints) {
        Map<Type, Set<Endpoint>> byType = new HashMap<Type, Set<Endpoint>>();
        for (Endpoint point : newEndpoints) {
            Type type = point.getType();

            Set<Endpoint> points = byType.get(type);
            if (points == null) {
                points = new HashSet<Endpoint>();
                byType.put(type, points);
            }

            points.add(point);
        }

        for(Type type : Type.values()) {
            Set<Endpoint> points = createImmutableSet(byType.get(type));
            byType.put(type, Collections.unmodifiableSet(points));
        }
        
        return Collections.unmodifiableMap(byType);
    }

    private Set<Endpoint> createImmutableSet(Set<Endpoint> endpoints) {
        if (endpoints == null)
            return NO_ENDPOINTS;
        else
            return Collections.unmodifiableSet(new HashSet<Endpoint>(endpoints));
    }
}
