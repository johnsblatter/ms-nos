package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;

public class FltPayload implements Message.Payload {

    private final Iden about;

    public FltPayload(Iden about) {
        this.about = about;
    }

    @Override
    public Message.Payload[] split() {
        return null;
    }

    public Iden getAbout() {
        return about;
    }
}
