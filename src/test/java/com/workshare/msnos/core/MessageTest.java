package com.workshare.msnos.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;

import com.workshare.msnos.core.payloads.PongPayload;
import com.workshare.msnos.core.payloads.TracePayload;

public class MessageTest {

    private static final Iden CLOUD_IDEN = new Iden(Iden.Type.CLD, UUID.randomUUID());
    private static final Iden AGENT_IDEN = new Iden(Iden.Type.AGT, UUID.randomUUID());
    private static final UUID MSG_UUID = UUID.randomUUID();

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToCreateAReliableMessageToTheCloud() {
        new MessageBuilder(Message.Type.APP, AGENT_IDEN, CLOUD_IDEN).withHops(1).reliable(true).make();
    }

    @Test
    public void shouldGenerateRndWhenSigned() {
        Message msg = new MessageBuilder(Message.Type.APP, CLOUD_IDEN, AGENT_IDEN).reliable(true).with(MSG_UUID).signed("12345", null).make();
        assertNotNull(msg.getRnd());
    }

    @Test
    public void shouldUsePassedRndWhenSigned() {
        Message msg = new MessageBuilder(Message.Type.APP, CLOUD_IDEN, AGENT_IDEN).reliable(true).with(MSG_UUID).signed("12345", "ABC").make();
        assertEquals("ABC", msg.getRnd());
    }

    @Test
    public void shouldGenerateUUIDIfUUIDEmpty() throws Exception {
        Message msg = new MessageBuilder(Message.Type.APP, AGENT_IDEN, CLOUD_IDEN).make();
        assertNotNull(msg.getUuid());
    }

    @Test
    public void shouldProvidePongPayloadOnPong() {
        Message msg = new MessageBuilder(Message.Type.PON, CLOUD_IDEN, AGENT_IDEN).make();
        assertNotNull(msg.getData());
        assertEquals(PongPayload.class, msg.getData().getClass());
    }

    @Test
    public void shouldProvideTracePayloadOnTrace() {
        Message msg = new MessageBuilder(Message.Type.TRC, CLOUD_IDEN, AGENT_IDEN).make();
        assertNotNull(msg.getData());
        assertEquals(TracePayload.class, msg.getData().getClass());
    }
}
