package com.workshare.msnos.core;

class Messages {

	public static Message presence(Agent agent, Cloud cloud) {
		return new Message(Message.Type.PRS, agent.getIden(), cloud.getIden(), 2, false, null);
	}
    public static Message discovery(Agent agent, Cloud cloud) {
		return new Message(Message.Type.DSC, agent.getIden(), cloud.getIden(), 2, false, null);
	}

}
