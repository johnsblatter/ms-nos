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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FltPayload that = (FltPayload) o;

        return !(about != null ? !about.equals(that.about) : that.about != null);
    }

    @Override
    public int hashCode() {
        return about != null ? about.hashCode() : 0;
    }

    @Override
    public boolean process(Message message, Internal internal) {
        return false;
    }

}
