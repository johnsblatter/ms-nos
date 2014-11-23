package com.workshare.msnos.core;

import java.util.UUID;

import com.workshare.msnos.soup.json.Json;

public class Iden {
    
    public static final Iden NULL = new Iden(Type.NUL, new UUID(0,0));
    
    public enum Type {NUL, AGT, CLD, MSG}

    private final Type type;
    private final UUID uuid;
    private final Long suid;

    public Iden(Type type, UUID uuid) {
        this(type, uuid, null);
    }

    public Iden(Type type, UUID uuid, Long suid) {
        this.type = type;
        this.uuid = uuid;
        this.suid = suid;
    }

    public Type getType() {
        return type;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Long getSuid() {
        return suid;
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((suid == null) ? 0 : suid.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        try {
            Iden other = (Iden) o;
            return this.type == other.type && areEquals(this.uuid, other.uuid) && areEquals(this.suid, other.suid);
        } catch (Exception ignore) {
            return false;
        }
    }
    
    public boolean equalsAsIdens(Object o) {
        try {
            Iden other = (Iden) o;
            return this.type == other.type && areEquals(this.uuid, other.uuid);
        } catch (Exception ignore) {
            return false;
        }
    }
    
    private boolean areEquals(Object o1, Object o2) {
        if (o1 == o2)
            return true;
        else if (o1 == null || o2 == null)
            return false;
        else 
            return o1.equals(o2);
    }
} 
