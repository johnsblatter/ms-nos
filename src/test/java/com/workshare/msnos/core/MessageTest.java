package com.workshare.msnos.core;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageTest {

    private static final Iden CLOUD_IDEN = new Iden(Iden.Type.CLD, UUID.randomUUID());
    private static final Iden AGENT_IDEN = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final UUID MSG_UUID = UUID.randomUUID();

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToCreateAReliableMessageToTheCloud() {
        new MessageBuilder(Message.Type.APP, AGENT_IDEN, CLOUD_IDEN, 1).with(1).reliable(true).make();
    }

    @Test
    public void shouldGenerateRndWhenSigned() {
        Message msg = new MessageBuilder(Message.Type.APP, CLOUD_IDEN, AGENT_IDEN, 1).reliable(true).with(MSG_UUID).signed("12345", null).make();
        assertNotNull(msg.getRnd());
    }

    @Test
    public void shouldUsePassedRndWhenSigned() {
        Message msg = new MessageBuilder(Message.Type.APP, CLOUD_IDEN, AGENT_IDEN, 1).reliable(true).with(MSG_UUID).signed("12345", "ABC").make();
        assertEquals("ABC", msg.getRnd());
    }

    @Test
    public void shouldGenerateUUIDFromSeqIfUUIDEmpty() throws Exception {
        int sequenceNumber = 19;
        Message msg = new MessageBuilder(Message.Type.APP, CLOUD_IDEN, AGENT_IDEN, sequenceNumber).make();
        assertNotNull(msg.getUuid());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotConstructIfNoUUIDOrSequence() throws Exception {
        Cloud cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, UUID.randomUUID()));

        Iden to = mock(Iden.class);
        when(to.getUUID()).thenReturn(UUID.randomUUID());

        new MessageBuilder(Message.Type.APP, cloud, to).make();
    }

    private UUID getUUID(Iden iden, long sequenceNumber) {
        return UUID.fromString(iden.getUUID().toString() + sequenceNumber);
    }
}
