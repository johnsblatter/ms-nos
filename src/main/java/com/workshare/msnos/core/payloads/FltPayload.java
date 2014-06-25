package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.soup.json.Json;

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

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
    public boolean process(Message message, Internal internal) {
        return false;
    }

}
