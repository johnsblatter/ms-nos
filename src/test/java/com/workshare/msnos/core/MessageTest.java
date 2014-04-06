package com.workshare.msnos.core;

import java.util.UUID;

import org.junit.Test;

public class MessageTest {

    private static final Iden CLOUD_IDEN = new Iden(Iden.Type.CLD, UUID.randomUUID());
	private static final Iden AGENT_IDEN = new Iden(Iden.Type.AGT, UUID.randomUUID());

	@Test (expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToCreateAReliableMessageToTheCloud() {
        new Message(Message.Type.APP, AGENT_IDEN, CLOUD_IDEN, 1, true, null);
    }
}
