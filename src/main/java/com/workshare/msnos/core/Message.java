package com.workshare.msnos.core;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;

import com.workshare.msnos.core.payloads.NullPayload;
import com.workshare.msnos.core.payloads.PongPayload;
import com.workshare.msnos.core.payloads.TracePayload;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.time.SystemTime;

public class Message {

    public interface Payload {
        public Payload[] split();

        public boolean process(Message message, Cloud.Internal internal);
    }

    public enum Status {
        UNKNOWN, PENDING, DELIVERED, FAILED
    }

    public enum Type {
        APP, PRS, DSC, PIN, PON, ACK, ENQ, FLT, QNE, HCK, TRC, CRT
    }

    private final Version version = Version.V1_0;
    private final UUID uuid;
    private final Type type;
    private final Iden from;
    private final Iden to;
    private final int hops;
    private final boolean reliable;
    private final long when;
    private final Payload data;
    private final String gateName;

    private final String sig;
    private final String rnd;

    private static final SecureRandom random = new SecureRandom();

    Message(Type type, Iden from, Iden to, int hops, boolean reliable, Payload data, UUID uuid, String sig, String rnd, long when, String gate) {
        if (reliable && to.getType() == Iden.Type.CLD) {
            throw new IllegalArgumentException("Cannot create a reliable message to the whole cloud!");
        }

        this.uuid = uuid == null ? UUID.randomUUID() : uuid;
        this.type = type;
        this.when = (when == 0 ? SystemTime.asMillis() : when);
        this.from = from;
        this.to = to;
        this.hops = hops;
        this.reliable = reliable;
        this.sig = sig;
        this.rnd = (sig == null ? null : (rnd == null ? new BigInteger(130, random).toString(32) : rnd));
        this.gateName = gate;
        
        // FIXME refactor this
        // TODO this can be achieved in a much better way (i.e.
        // type.getPayload(this) here or in the builder)
        if (data == null) {
            if (type == Type.TRC)
                this.data = new TracePayload(from);
            else if (type == Type.PON)
                this.data = new PongPayload();
            else
                this.data = NullPayload.INSTANCE;
        } else {
            this.data = data;
        }
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

    public long getWhen() {
        return when;
    }

    public String getReceivingGate() {
        return gateName;
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

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (sig != null ? sig.hashCode() : 0);
        result = 31 * result + (rnd != null ? rnd.hashCode() : 0);
        return result;
    }

    public Message data(Payload load) {
        return new Message(type, from, to, hops, reliable, load, uuid, sig, rnd, when, gateName);
    }

    public Message hopped() {
        return new Message(type, from, to, hops - 1, reliable, data, uuid, sig, rnd, when, gateName);
    }

    public Message withHops(int hops) {
        if (hops == this.hops)
            return this;
        else
            return new Message(type, from, to, hops, reliable, data, uuid, sig, rnd, when, gateName);
    }

    public Message signed(String keyId, String signature) {
        String sign = keyId + ":" + signature;
        return new Message(type, from, to, hops, reliable, data, uuid, sign, rnd, when, gateName);
    }

    public Message fromGate(String newGateName) {
        if (newGateName != null || gateName != null) {
            return new Message(type, from, to, hops - 1, reliable, data, uuid, sig, rnd, when, newGateName);
        } else {
            return this;
        }
    }

}
