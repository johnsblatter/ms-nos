package com.workshare.msnos.core;

import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.cloud.Cloud;

import java.util.UUID;

public class MessageBuilder {

    private final Type type;
    private final Iden from;
    private final Iden to;

    private UUID uuid = null;
    private int hops = 3;
    private boolean reliable = false;
    private Payload data = null;

    private String sig = null;
    private String rnd = null;
    private long when;
    private String gateName;

    public MessageBuilder(Type type, Cloud from, Identifiable to) {
        this(type, from, to.getIden());
    }

    public MessageBuilder(Type type, Cloud from, Iden to) {
        this(type, from.getIden(), to);
    }

    public MessageBuilder(Type type, Agent from, Identifiable to) {
        this(type, from.getIden(), to.getIden());
    }

    public MessageBuilder(Type type, Iden from, Iden to) {
        this.type = type;
        this.from = from;
        this.to = to;
    }

    public MessageBuilder with(UUID uuid) {
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

    public MessageBuilder withGateName(String name) {
        this.gateName = name;
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

    public MessageBuilder at(long when) {
        this.when = when;
        return this;
    }

    public Message make() {
        if (from == null)
            throw new RuntimeException("Cannot build a message with no source");
        if (to == null)
            throw new RuntimeException("Cannot build a message with no destination");

        return new Message(type, from, to, hops, reliable, data, uuid, sig, rnd, when, gateName);
    }
}
