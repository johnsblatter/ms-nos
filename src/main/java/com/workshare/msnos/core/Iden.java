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
        return this.uuid.hashCode();
    }

    public boolean equals(Object o) {
        try {
            Iden other = (Iden) o;
            return this.type == other.type && this.uuid.equals(other.uuid);
        } catch (Exception ignore) {
            return false;
        }
    }
} 
