package com.workshare.msnos.core;

import java.util.UUID;

import com.workshare.msnos.soup.json.Json;

/**
 * A unique identifier for an entity in the msnos system. 
 * It's composed by a type (AGT, CLD, MSG) and a unique id (an UUID)
 * 
 * @author bbossola
 */
public class Iden {
    
    public static final Iden NULL = new Iden(Type.NUL, new UUID(0,0));
   
    /**
     * enum of all entities type
     */
    public enum Type {NUL, AGT, CLD, MSG}

    private final Type type;
    private final UUID uuid;

    /**
     * Creates an unique identifier
     * 
     * @param type  the type
     * @param uuid  the uuid
     */
    public Iden(Type type, UUID uuid) {
        this.type = type;
        this.uuid = uuid;       
    }

    /**
     * Returns the type of this identifier
     * 
     * @return  the type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the uuid of this identifier
     * 
     * @return  the uuid
     */
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
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
