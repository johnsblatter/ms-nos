package com.workshare.msnos.core;

import java.util.UUID;

import com.workshare.msnos.soup.json.Json;

public class Agent {

    private final Iden iden;

    private Cloud cloud;

    public Agent(UUID uuid) {
        this.iden = new Iden(Iden.Type.AGT, uuid);
    }

    public Iden getIden() {
        return iden;
    }

    public Agent join(Cloud cloud) {
        this.cloud = cloud;
        return this;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public boolean isForMe(Message message) {
//        BRUNO TOLD ME SO...
        return message.getTo().equals(getIden()) || message.getTo().equals(getCloud().getIden());
    }
    
    public String toString() {
        return Json.toJsonString(this);
    }
}
