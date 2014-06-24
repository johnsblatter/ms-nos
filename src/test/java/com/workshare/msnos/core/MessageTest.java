package com.workshare.msnos.core;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;

public class MessageTest {

    private static final Iden CLOUD_IDEN = new Iden(Iden.Type.CLD, UUID.randomUUID());
    private static final Iden AGENT_IDEN = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final UUID MSG_UUID = UUID.randomUUID();

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToCreateAReliableMessageToTheCloud() {
        new Message(Message.Type.APP, AGENT_IDEN, CLOUD_IDEN, 1, true, null);
    }
    
    @Test
    public void shouldGenerateRndWhenSigned() {
        Message msg = new Message(Message.Type.APP, CLOUD_IDEN, AGENT_IDEN, 1, true, null, MSG_UUID, "12345", null);
        assertNotNull(msg.getRnd());
    }
    
    @Test
    public void shouldUsePassedRndWhenSigned() {
        Message msg = new Message(Message.Type.APP, CLOUD_IDEN, AGENT_IDEN, 1, true, null, MSG_UUID, "12345", "ABC");
        assertEquals("ABC", msg.getRnd());
    }
}
