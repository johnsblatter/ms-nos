package com.workshare.msnos.core;

import java.util.UUID;

import org.junit.Test;

public class MessageTest {

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToCreateAReliableMessageToTheCloud() {
        final UUID uuid = new UUID(123, 456);
        final Iden agent = new Iden(Iden.Type.AGT, uuid);
        final Iden cloud = new Iden(Iden.Type.CLD, uuid);
        new Message(Message.Type.APP, agent, cloud, "sigval", 1, true, null);
    }
}
