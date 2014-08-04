package com.workshare.msnos.core.serializers;

import com.workshare.msnos.core.*;
import com.workshare.msnos.core.cloud.JoinSynchronizer;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.usvc.api.RestApi;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class WireJsonSerializerTest {

    private static final UUID CLOUD_UUID = UUID.randomUUID();
    private static final UUID AGENT_UUID = UUID.randomUUID();

    private static final Iden CLOUD_IDEN = new Iden(Iden.Type.CLD, CLOUD_UUID);
    private static final Iden AGENT_IDEN = new Iden(Iden.Type.AGT, AGENT_UUID);

    private WireJsonSerializer sz = new WireJsonSerializer();
    private Cloud cloud;
    private LocalAgent agent;

    @Before
    public void before() throws Exception {
        cloud = new Cloud(CLOUD_UUID, null, new HashSet<Gateway>(Arrays.asList(new NoopGateway())), Mockito.mock(JoinSynchronizer.class));

        agent = new LocalAgent(AGENT_UUID);
        agent.join(cloud);
    }

    @Test
    public void shouldSerializeCloudObject() throws Exception {

        String expected = "\"CLD:" + toShortString(CLOUD_UUID) + "\"";
        String current = sz.toText(cloud);

        assertEquals(expected, current);
    }

    private String toShortString(UUID uuid) {
        return uuid.toString().replaceAll("-", "");
    }

    @Test
    public void shouldDeserializeCloudObject() throws Exception {
        Cloud expected = cloud;
        Cloud current = sz.fromText("\"CLD:" + toShortString(CLOUD_UUID) + "\"", Cloud.class);

        assertEquals(expected.getIden(), current.getIden());
    }

    @Test
    public void shouldSerializeAgentObject() throws Exception {
        String expected = "\"AGT:" + toShortString(AGENT_UUID) + "\"";
        String current = sz.toText(agent);

        assertEquals(expected, current);
    }

    @Test
    public void shouldDeserializeAgentObject() throws Exception {
        LocalAgent current = sz.fromText("\"AGT:" + toShortString(AGENT_UUID) + "\"", LocalAgent.class);

        assertEquals(agent.getIden(), current.getIden());
    }

    @Test
    public void shouldBeAbleToEncodeAndDecodeMessage() throws Exception {
        Message source = new MessageBuilder(Message.Type.PRS, agent, cloud).with(new Presence(true)).make();

        byte[] data = sz.toBytes(source);
        Message decoded = sz.fromBytes(data, Message.class);

        assertEquals(source, decoded);
    }

    @Test
    public void shouldBeAbleToEncodeAndDecodeQNE() throws Exception {
        Message source = new MessageBuilder(Message.Type.QNE, agent, cloud).with(new QnePayload("test", new RestApi("test", "/test", 7070))).make();

        byte[] data = sz.toBytes(source);
        Message decoded = sz.fromBytes(data, Message.class);

        assertEquals(source, decoded);
    }

    @Test
    public void shouldSerializeVersionObject() throws Exception {
        String expected = "\"1.0\"";
        String current = sz.toText(Version.V1_0);

        assertEquals(expected, current);
    }

    @Test
    public void shouldSerializeUUIDObject() throws Exception {
        UUID uuid = UUID.randomUUID();
        String expected = "\"" + toShortString(uuid).replace("-", "") + "\"";
        String current = sz.toText(uuid);

        assertEquals(expected, current);
    }

    @Test
    public void shouldDeserializeUUIDObject() throws Exception {
        UUID expected = UUID.randomUUID();
        String text = "\"" + toShortString(expected).replace("-", "") + "\"";
        UUID current = sz.fromText(text, UUID.class);

        assertEquals(expected, current);
    }

    @Test
    public void shouldBeAbleToEncodeAndDecodeSignedMessage() throws Exception {
        final String sig = "this-is-a-signature";
        final String rnd = "random";
        Message source = new MessageBuilder(Message.Type.QNE, agent, cloud).signed(sig, rnd).make();

        byte[] data = sz.toBytes(source);
        Message decoded = sz.fromBytes(data, Message.class);

        assertEquals(sig, decoded.getSig());
        assertEquals(rnd, decoded.getRnd());
    }

    @Test
    public void shouldNotSerializeUUIDIfSequenceNumber() throws Exception {
        Message source = new MessageBuilder(Message.Type.QNE, agent, cloud).with(UUID.randomUUID()).sequence(23).make();

        byte[] data = sz.toBytes(source);
        Message decoded = sz.fromBytes(data, Message.class);

        assertEquals(getUUID(agent.getIden(), 23), decoded.getUuid());
    }

    private UUID getUUID(Iden iden, long sequenceNumber) {
        return UUID.fromString(iden.getUUID().toString() + sequenceNumber);
    }
}
