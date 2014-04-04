package com.workshare.msnos.core;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;

import com.workshare.msnos.core.protocols.ip.udp.Utils;
import com.workshare.msnos.soup.json.Json;

public class MessageTest {

    final Agent agent = new Agent(new UUID(111, 333));
    final Cloud cloud = new Cloud(new UUID(222, 444));

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToCreateAReliableMessageToTheCloud() {
        new Message(Message.Type.APP, agent.getIden(), cloud.getIden(), "sigval", 1, true, null);
    }

    @Test
    public void shouldBeAbleToEncodeAndDecode(){
        Message source = Utils.newSampleMessage().from(agent.getIden()).to(cloud.getIden());
        byte[] data = Json.toBytes(source);

        Message decoded = Json.fromBytes(data, Message.class);
        
        assertEquals(source,decoded);
    }

}
