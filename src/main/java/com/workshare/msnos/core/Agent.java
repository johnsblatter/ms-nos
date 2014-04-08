package com.workshare.msnos.core;

import com.google.gson.JsonObject;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.soup.json.Json;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;

public class Agent implements Identifiable {

    // FIXME this cannot be hardcoded!!!
    public static final int DEFAULT_HOPS = 3;

    private final Iden iden;
    private transient Cloud cloud;

    public Agent(UUID uuid) {
        this.iden = new Iden(Iden.Type.AGT, uuid);
    }

    Agent(Iden iden, Cloud cloud) {
        validate(iden, cloud);
        this.iden = iden;
        this.cloud = cloud;
    }

    private void validate(Iden iden, Cloud cloud) {
        if (cloud == null)
            throw new IllegalArgumentException("Invalid cloud");
        if (iden == null || iden.getType() != Iden.Type.AGT)
            throw new IllegalArgumentException("Invalid iden");
    }

    public Iden getIden() {
        return iden;
    }

    public Agent join(Cloud cloud) throws IOException {
        this.cloud = cloud;
        cloud.onJoin(this);
        return this;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public boolean isForMe(Message message) {
        return message.getTo().equals(getIden()) || message.getTo().equals(getCloud().getIden());
    }
    
    public void leave(Cloud cloud) {
        // TODO Auto-generated method stub
    }
    
    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
	public boolean equals(Object other) {
	    try {
	        return this.iden.equals(((Agent)(other)).getIden());
	    } catch (Exception any) {
	        return false;
	    }
	}
	
    @Override
	public int hashCode() {
	    return iden.hashCode();
	}

    public Future<Status> sendMessage(Identifiable to, Type type, JsonObject data) throws IOException {
        return cloud.send(new Message(type, this.getIden(), to.getIden(), DEFAULT_HOPS, false, data));
    }

    public Future<Status> sendReliableMessage(Identifiable to, Type type, JsonObject data) throws IOException {
        return cloud.send(new Message(type, this.getIden(), to.getIden(), DEFAULT_HOPS, true, data));
    }
}
