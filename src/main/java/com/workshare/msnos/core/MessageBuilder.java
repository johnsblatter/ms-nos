package com.workshare.msnos.core;

import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.soup.time.SystemTime;

import java.util.UUID;

public class MessageBuilder {

    private final Type type;
    private final Iden from;
    private final Iden to;

    private UUID uuid = null;
    private int hops = 2;
    private long seq;
    private boolean reliable = false;
    private Payload data = null;

    private String sig = null;
    private String rnd = null;

    public MessageBuilder(Type type, Iden from, Iden to) {
        this.type = type;
        this.from = from;
        this.to = to;
    }

    public MessageBuilder(Type type, Identifiable from, Identifiable to) {
        this.type = type;
        this.from = from.getIden();
        this.to = to.getIden();
    }

    public MessageBuilder with(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public MessageBuilder with(int hops) {
        this.hops = hops;
        return this;
    }

    public MessageBuilder reliable(boolean reliable) {
        this.reliable = reliable;
        return this;
    }

    public MessageBuilder with(Payload data) {
        this.data = data;
        return this;
    }

    public MessageBuilder signed(String sig, String rnd) {
        this.sig = sig;
        this.rnd = rnd;
        return this;
    }

    public MessageBuilder sequence(long seq) {
        this.seq = seq;
        return this;
    }

    public Message make() {
        if (from == null)
            throw new RuntimeException("Cannot build a message with no source");
        if (to == null)
            throw new RuntimeException("Cannot build a message with no destination");
        if (uuid == null)
            uuid = UUID.randomUUID();
        if (seq == 0)
            seq = SystemTime.asMillis();

        return new Message(type, from, to, hops, reliable, data, uuid, sig, rnd, seq);
    }
}
