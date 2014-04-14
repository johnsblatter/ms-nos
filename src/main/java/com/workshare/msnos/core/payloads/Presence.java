package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Message;

public class Presence implements Message.Payload {
    private final boolean present;

    public Presence(boolean present) {
        this.present = present;
    }

    public boolean isPresent() {
        return present;
    }
}
