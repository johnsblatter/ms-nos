package com.workshare.msnos.core;

import java.util.UUID;

import com.workshare.msnos.soup.json.Json;

public class Iden {
    public enum Type {AGT, CLD, MSG}

    private final Type type;
    private final UUID uuid;

    public Iden(Type type, UUID uuid) {
        this.type = type;
        this.uuid = uuid;
    }

    public Type getType() {
        return type;
    }

    public UUID getUUID() {
        return uuid;
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
