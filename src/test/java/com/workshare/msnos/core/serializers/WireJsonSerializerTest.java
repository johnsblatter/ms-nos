package com.workshare.msnos.core.serializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.NoopGateway;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.RemoteEntity;
import com.workshare.msnos.core.Version;
import com.workshare.msnos.core.cloud.JoinSynchronizer;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.HealthcheckPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.usvc.api.RestApi;

public class WireJsonSerializerTest {

    private static final UUID AGENT_UUID = UUID.randomUUID();

    private static final UUID CLOUD_UUID = UUID.randomUUID();
    private static final Long CLOUD_INSTANCE_ID = 1274L;

    private static final Iden A_CLOUD_IDEN = new Iden(Iden.Type.CLD, UUID.randomUUID());

    private static final Network SAMPLE_NETWORK = new Network(new byte[]{10,10,10,1}, (short)25);

    private Cloud cloud;
    private LocalAgent localAgent;
    private RemoteEntity remoteAgent;
    private WireJsonSerializer sz = new WireJsonSerializer();

    @BeforeClass
    public static void useLocalTimeSource() {
        System.setProperty("com.ws.nsnos.time.local", "true");
    }

    @Before
    public void before() throws Exception {
        cloud = new Cloud(CLOUD_UUID, "1231", new HashSet<Gateway>(Arrays.asList(new NoopGateway())), mock(JoinSynchronizer.class), CLOUD_INSTANCE_ID);

        localAgent = new LocalAgent(AGENT_UUID);
        localAgent.join(cloud);

        remoteAgent = new RemoteAgent(UUID.randomUUID(), cloud, null);
    }

    @Test
    public void shouldBeAbleToEncodeAndDecodeMessage() throws Exception {
        Message source = new MessageBuilder(Message.Type.PRS, localAgent, remoteAgent).with(new Presence(true, localAgent)).make();

        byte[] data = sz.toBytes(source);
        Message decoded = sz.fromBytes(data, Message.class);

        assertEquals(source, decoded);
    }

    @Test
    public void shouldBeAbleToEncodeAndDecodeQNE() throws Exception {
        Message source = new MessageBuilder(Message.Type.QNE, localAgent, remoteAgent).with(new QnePayload("test", new RestApi("/test", 7070))).make();

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
        Message source = new MessageBuilder(Message.Type.QNE, localAgent, cloud).signed(sig, rnd).make();

        byte[] data = sz.toBytes(source);
        Message decoded = sz.fromBytes(data, Message.class);

        assertEquals(sig, decoded.getSig());
        assertEquals(rnd, decoded.getRnd());
    }

    @Test
    public void shouldCorrectlyDeserializeFLTMessage() throws Exception {
        Message source = new MessageBuilder(MessageBuilder.Mode.RELAXED, Message.Type.FLT, A_CLOUD_IDEN, A_CLOUD_IDEN).with(new FltPayload(localAgent.getIden())).with(UUID.randomUUID()).make();

        byte[] data = sz.toBytes(source);
        Message decoded = sz.fromBytes(data, Message.class);

        assertEquals(source.getData(), decoded.getData());
    }

    @Test
    public void shouldSerializeBooleanCompact() throws Exception {
        assertEquals("1", sz.toText(Boolean.TRUE));
        assertEquals("0", sz.toText(Boolean.FALSE));
    }

    @Test
    public void shouldDeserializeBooleanCompact() throws Exception {
        assertEquals(Boolean.TRUE, sz.fromText("1", Boolean.class));
        assertEquals(Boolean.FALSE, sz.fromText("0", Boolean.class));
    }

    @Test
    public void shouldMessageFromCloudContainExtendedIden() throws Exception {
        Message source = new MessageBuilder(Message.Type.PIN, cloud, localAgent).with(UUID.randomUUID()).make();

        String expected = "\"fr\":\"CLD:" + toShortString(CLOUD_UUID) + ":" + Long.toString(CLOUD_INSTANCE_ID, 32) + "\"";
        String current = sz.toText(source);

        assertTrue(current.contains(expected));
    }

    @Test
    public void shouldMessageToCloudContainStandardIden() throws Exception {
        Message source = new MessageBuilder(Message.Type.PON, localAgent, cloud).make();

        String expected = "\"to\":\"CLD:" + toShortString(CLOUD_UUID) + "\"";
        String current = sz.toText(source);

        assertTrue(current.contains(expected));
    }

    @Test
    public void shouldSerializeBaseEndpoint() throws Exception {
        Endpoint expected = new BaseEndpoint(Endpoint.Type.SSH, SAMPLE_NETWORK);
        Endpoint current = sz.fromText(sz.toText(expected), Endpoint.class);
        assertEquals(expected, current);
    }

    @Test
    public void shouldSerializeHTTPEndpoint() throws Exception {
        Endpoint expected = new HttpEndpoint(SAMPLE_NETWORK, "http://www.workshare.com");
        Endpoint current = sz.fromText(sz.toText(expected), Endpoint.class);
        assertEquals(expected, current);
    }

    @Test
    public void shouldCorrectlyDeserializeHCKMessage() throws Exception {
        Payload payload = new HealthcheckPayload(localAgent, true);
        Message source = new MessageBuilder(MessageBuilder.Mode.RELAXED, Message.Type.HCK, A_CLOUD_IDEN, A_CLOUD_IDEN).with(payload).with(UUID.randomUUID()).make();

        byte[] data = sz.toBytes(source);
        Message decoded = sz.fromBytes(data, Message.class);

        assertEquals(source.getData(), decoded.getData());
    }


    private String toShortString(UUID uuid) {
        return uuid.toString().replaceAll("-", "");
    }

}
