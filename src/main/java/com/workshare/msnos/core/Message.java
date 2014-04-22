package com.workshare.msnos.core;

import com.workshare.msnos.soup.json.Json;

import java.util.UUID;

public class Message {

    public interface Payload {
        public Payload[] split();
    }

    public enum Status {UNKNOWN, PENDING, DELIVERED}

    public enum Type {PRS, DSC, APP, PIN, PON, ACK, ENQ, QNE}

    private final Version version = Version.V1_0;
    private final UUID uuid;
    private final Type type;
    private final Iden from;
    private final Iden to;
    private final String sig;
    private final int hops;
    private final boolean reliable;
    private final Payload data;

    public Message(Type type, Iden from, Iden to, int hops, boolean reliable, Payload data) {
        if (reliable && to.getType() == Iden.Type.CLD) {
            throw new IllegalArgumentException("Cannot create a reliable message to the cloud!");
        }

        this.type = type;
        this.from = from;
        this.to = to;
        this.sig = null;        // FIXME TODO let's remember to add security, shall we? what about .signedWith(...)
        this.hops = hops;
        this.reliable = reliable;
        this.data = data;
        this.uuid = UUID.randomUUID();
    }

    public Message(Type type, Iden from, Iden to, int hops, boolean reliable, Payload data, UUID uuid) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.sig = null;        // FIXME TODO let's remember to add security, shall we? what about .signedWith(...)
        this.hops = hops;
        this.reliable = reliable;
        this.data = data;
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
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

    public Payload getData() {
        return data;
    }

    public int getHops() {
        return hops;
    }

    public boolean isReliable() {
        return reliable;
    }

    public Message from(Iden from) {
        return new Message(type, from, to, hops, reliable, data);
    }

    public Message to(Iden to) {
        return new Message(type, from, to, hops, reliable, data);
    }

    public Message reliable() {
        return new Message(type, from, to, hops, true, data);
    }

    public Message data(Payload load) {
        return new Message(type, from, to, hops, reliable, load, uuid);

    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
    public boolean equals(Object o) {
        try {
            String jsonThis = Json.toJsonString(this);
            String jsonThat = Json.toJsonString(o);
            return jsonThis.equals(jsonThat);
        } catch (Exception any) {
            return false;
        }
    }

}
