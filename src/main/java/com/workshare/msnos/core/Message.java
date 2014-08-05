package com.workshare.msnos.core;

import com.workshare.msnos.core.payloads.NullPayload;
import com.workshare.msnos.core.payloads.PongPayload;
import com.workshare.msnos.soup.json.Json;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;

public class Message {

    public interface Payload {
        public Payload[] split();

        public boolean process(Message message, Cloud.Internal internal);
    }

    public enum Status {UNKNOWN, PENDING, DELIVERED}

    public enum Type {PRS, DSC, APP, PIN, PON, ACK, ENQ, FLT, QNE}

    private final Version version = Version.V1_0;
    private final UUID uuid;
    private final Type type;
    private final Iden from;
    private final Iden to;
    private final int hops;
    private final long seq;
    private final boolean reliable;
    private final Payload data;

    private final String sig;
    private final String rnd;

    private static final SecureRandom random = new SecureRandom();

    Message(Type type, Iden from, Iden to, int hops, boolean reliable, Payload data, UUID uuid, String sig, String rnd, long seq) {
        if (reliable && to.getType() == Iden.Type.CLD) {
            throw new IllegalArgumentException("Cannot create a reliable message to the whole cloud!");
        }
        if (seq == 0 && uuid == null) {
            throw new IllegalArgumentException("Unable to construct message without UUID or Sequence number!");
        }

        this.uuid = uuid == null ? UUID.nameUUIDFromBytes((from.getUUID().toString() + seq).getBytes()) : uuid;
        this.type = type;
        this.from = from;
        this.to = to;
        this.hops = hops;
        this.reliable = reliable;
        this.sig = sig;
        this.rnd = (sig == null ? null : (rnd == null ? new BigInteger(130, random).toString(32) : rnd));
        this.seq = seq;

        if (type == Type.PON)
            this.data = new PongPayload();
        else
            this.data = (data == null ? NullPayload.INSTANCE : data);
    }

    public long getSeq() {
        return seq;
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

    public String getRnd() {
        return rnd;
    }

    public boolean isReliable() {
        return reliable;
    }

    public Message reliable() {
        return new Message(type, from, to, hops, true, data, uuid, sig, rnd, seq);
    }

    public Message data(Payload load) {
        return new Message(type, from, to, hops, reliable, load, uuid, sig, rnd, seq);

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

    public Message signed(String keyId, String signature) {
        String sign = keyId + ":" + signature;
        return new Message(type, from, to, hops, reliable, data, uuid, sign, rnd, seq);
    }

}
