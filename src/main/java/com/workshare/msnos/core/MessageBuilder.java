package com.workshare.msnos.core;

import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.Message.Type;

import java.util.UUID;

public class MessageBuilder {

    public enum Mode {RELAXED, STRICT}

    private final Type type;
    private final Iden from;
    private final Iden to;

    private Mode mode;

    private UUID uuid = null;
    private int hops = 3;
    private long seq;
    private boolean reliable = false;
    private Payload data = null;

    private String sig = null;
    private String rnd = null;

    public MessageBuilder(Type type, Cloud from, Identifiable to) {
        this(type, from, to.getIden());
    }

    public MessageBuilder(Type type, Cloud from, Iden to) {
        this(Mode.STRICT, type, from.getIden(), to);
        this.seq = from.getNextSequence();
    }

    public MessageBuilder(Type type, Agent from, Identifiable to) {
        this(Mode.STRICT, type, from.getIden(), to.getIden());
        this.seq = from.getNextSequence();
    }

    public MessageBuilder(Mode mode, Type type, Iden from, Iden to) {
        this.mode = mode;
        this.type = type;
        this.from = from;
        this.to = to;
    }

    public MessageBuilder with(UUID uuid) {
        if (isStrict() && from.getType() == Iden.Type.AGT)
            throw new IllegalArgumentException("Cannot accept a UUID if the message is sent from an agent!");

        this.uuid = uuid;
        return this;
    }

    public MessageBuilder with(Payload data) {
        this.data = data;
        return this;
    }

    public MessageBuilder withHops(int hops) {
        this.hops = hops;
        return this;
    }

    public MessageBuilder sequence(long seqnum) {
        if (isStrict())
            throw new IllegalArgumentException("Cannot accept a sequence number, it's taken from the source!");

        this.seq = seqnum;
        return this;
    }

    public MessageBuilder reliable(boolean reliable) {
        this.reliable = reliable;
        return this;
    }

    public MessageBuilder signed(String sig, String rnd) {
        this.sig = sig;
        this.rnd = rnd;
        return this;
    }

    private boolean isStrict() {
        return mode == Mode.STRICT;
    }

    public Message make() {
        if (from == null)
            throw new RuntimeException("Cannot build a message with no source");
        if (to == null)
            throw new RuntimeException("Cannot build a message with no destination");

        return new Message(type, from, to, hops, reliable, data, uuid, sig, rnd, seq);
    }

}
