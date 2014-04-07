package com.workshare.msnos.core;

import java.io.IOException;
import java.util.UUID;

import com.workshare.msnos.soup.json.Json;

public class Agent implements Identifiable {

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
//        BRUNO TOLD ME SO...
        return message.getTo().equals(getIden()) || message.getTo().equals(getCloud().getIden());
    }
    
    public String toString() {
        return Json.toJsonString(this);
    }

	public void leave(Cloud cloud) {
		// TODO Auto-generated method stub
	}
	
	public boolean equals(Object other) {
	    try {
	        return this.iden.equals(((Agent)(other)).getIden());
	    } catch (Exception any) {
	        return false;
	    }
	}
	
	public int hashCode() {
	    return iden.hashCode();
	}
}
