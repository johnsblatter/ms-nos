package com.workshare.msnos.core;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;

public class Ring {

    private static Logger log = LoggerFactory.getLogger(Ring.class);

    private final UUID uuid;

    private Ring(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }
    
    
    @Override
    public String toString() {
        return "ring-"+uuid.toString();
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Ring other = (Ring) obj;
            return other.uuid.equals(this.uuid);
        }
        catch (Exception any) {
            return false;
        }
    }

    public static Ring make(Set<Endpoint> endpoints) {
        Endpoint point = null;
        if (endpoints != null)
            for (Endpoint endpoint : endpoints) {
                point = endpoint;
                if (endpoint.getType() == Type.UDP)
                    break;
            }
        
        UUID uid;
        if (point != null) {
            final long most = toLong(point.getNetwork().getAddress());
            final long least = point.getNetwork().getPrefix();
            uid = new UUID(most, least);
        } else {
            uid = UUID.randomUUID();
            log.warn("Random ring is being generated as no endpoints are available: {}", endpoints);
        }
        
        return new Ring(uid);
    }

    public static Ring random() {
        return new Ring(UUID.randomUUID());
    }

    private static long toLong(byte[] values) {
        long shift = 0;
        long total = 0;
        for (byte value : values) {
            final long lval = (int)(value&0xff);
            total = total + lval << shift;
            shift += 8;
        }
        
        return total;
    }

}

