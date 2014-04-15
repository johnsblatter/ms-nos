package com.workshare.msnos.core.serializers;

import com.workshare.msnos.core.*;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.udp.Utils;
import com.workshare.msnos.soup.json.Json;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class WireJsonSerializerTest {

    private static final UUID CLOUD_UUID = UUID.randomUUID();
    private static final UUID AGENT_UUID = UUID.randomUUID();

    private static final Iden CLOUD_IDEN = new Iden(Iden.Type.CLD, CLOUD_UUID);
    private static final Iden AGENT_IDEN = new Iden(Iden.Type.AGT, AGENT_UUID);

    private WireJsonSerializer sz = new WireJsonSerializer();
    private Cloud cloud;
    private Agent agent;

    @Before
    public void before() throws Exception {
        cloud = new Cloud(CLOUD_UUID, new HashSet<Gateway>(Arrays.asList(new NoopGateway())), new Cloud.Multicaster());

        agent = new Agent(AGENT_UUID);
        agent.join(cloud);
    }

    @Test
    public void shouldSerializeCloudObject() throws Exception {

        String expected = "\"CLD:" + CLOUD_UUID.toString() + "\"";
        String current = sz.toText(cloud);

        assertEquals(expected, current);
    }

    @Test
    public void shouldDeserializeCloudObject() throws Exception {
        Cloud expected = cloud;
        Cloud current = sz.fromText("\"CLD:" + CLOUD_UUID.toString() + "\"", Cloud.class);

        assertEquals(expected.getIden(), current.getIden());
    }

    @Test
    public void shouldSerializeAgentObject() throws Exception {
        String expected = "\"AGT:" + AGENT_UUID.toString() + "\"";
        String current = sz.toText(agent);

        assertEquals(expected, current);
    }

    @Test
    public void shouldDeserializeAgentObject() throws Exception {
        Agent current = sz.fromText("\"AGT:" + AGENT_UUID.toString() + "\"", Agent.class);

        assertEquals(agent.getIden(), current.getIden());
    }

    @Test
    public void shouldBeAbleToEncodeAndDecodeMessage() throws Exception {
        Message source = new Message(Message.Type.PRS, AGENT_IDEN, CLOUD_IDEN, 2, false, new Presence(true));

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

    public static void main(String[] args) throws IOException {
        WireJsonSerializer sz = new WireJsonSerializer();

        Message source = Utils.newSampleMessage().from(AGENT_IDEN).to(CLOUD_IDEN);
        byte[] data = sz.toBytes(source);

        Message decoded = sz.fromBytes(data, Message.class);
        System.out.println(Json.toJsonString(decoded));
        System.out.println(sz.toText(source));
    }

    private static Set<Network> getNetworks() throws SocketException {
        Set<Network> nets = new HashSet<Network>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                nets.addAll(Network.list(nic));
            }
        } catch (SocketException e) {
            System.out.println("WireJsonSerializerTest.getNetworks" + e);
            throw e;
        }
        return nets;
    }
}
