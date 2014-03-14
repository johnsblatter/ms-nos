package com.workshare.msnos.core;

import com.google.gson.JsonObject;
import com.workshare.msnos.json.Json;

public class Message {
    
    public enum Status {UNKNOWN, PENDING, DELIVERED}

    public enum Type {PRS, DSC, APP}

    private final Version version = Version.V1_0;
    private final Type type;
    private final Iden from;
    private final Iden to;
    private final String sig;
    private final int hops;
    private final boolean reliable;
    private final JsonObject data;
    
    public Message(Type type, Iden from, Iden to, String sig, int hops, boolean reliable, JsonObject data) {
        if (reliable && to.getType() == Iden.Type.CLD)
            throw new IllegalArgumentException("Cannot create a reliable message to the cloud!");
        
        this.type = type;
        this.from = from;
        this.to = to;
        this.sig = sig;
        this.hops = hops;
        this.reliable = reliable;
        this.data = data;
    }

    public Version getVersion() {
        return version;
    }

    public Type getType() {
        return type;
    }

    public Iden getFrom() {
        return from;
    }

    public Iden getTo() {
        return to;
    }

    public String getSig() {
        return sig;
    }

    public JsonObject getData() {
        return data;
    }

    public int getHops() {
        return hops;
    }

    public boolean isReliable() {
        return reliable;
    }

    public String toString() {
        return Json.toJsonString(this);
    }
}
