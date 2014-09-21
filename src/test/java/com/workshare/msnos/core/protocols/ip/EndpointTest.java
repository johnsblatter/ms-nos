package com.workshare.msnos.core.protocols.ip;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class EndpointTest {

    @Test
    public void shouldBeSerializableToJson() {
        Network network = new Network(new byte[]{25,25,25,25}, (short)4);
        Endpoint endpoint = new BaseEndpoint(Endpoint.Type.HTTP, network, (short)80);
        assertNotNull(endpoint.toString());
    }
}
