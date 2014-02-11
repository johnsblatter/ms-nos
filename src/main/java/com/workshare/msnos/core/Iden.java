package com.workshare.msnos.core;

import java.util.UUID;

import com.workshare.msnos.json.Json;

public class Iden {
    public enum Type {AGT, CLD, MSG}

    private final Type type;
    private final UUID uuid;;
    
    public Iden(Type type, UUID uuid) {
        this.type = type;
        this.uuid = uuid;
    }

    public String toString() {
        return Json.toJsonString(this);
    }

    public int hashcode() {
        return toString().hashCode();
    }
    
    public boolean equals(Object o) {
        try {
            Iden other = (Iden)o;
            return this.type == other.type && other.uuid.equals(other.uuid);
        } catch (Exception ignore) {
            return false;
        }
    }
} 
