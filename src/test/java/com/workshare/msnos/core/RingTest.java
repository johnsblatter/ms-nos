package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.asNetwork;
import static com.workshare.msnos.core.CoreHelper.asPublicNetwork;
import static com.workshare.msnos.core.CoreHelper.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;

import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;

public class RingTest {

    @Test
    public void shouldCalculateRingBasedOnUDPEndpoint() {
        Endpoint ssh = new BaseEndpoint(Type.SSH, asPublicNetwork("25.25.25.25"));
        Endpoint htp = new HttpEndpoint(asPublicNetwork("25.25.25.25"), "http://25.25.25.25");
        Endpoint udp = new BaseEndpoint(Type.UDP, asNetwork("192.168.0.199",(short)16));

        Ring ring = Ring.make(asSet(udp, ssh, htp));
        
        long v1 = toLong(udp.getNetwork().getAddress());
        long v2 = udp.getNetwork().getPrefix();
        assertEquals(new UUID(v1,v2), ring.uuid());
    }

    @Test
    public void shouldReturnRandomRingWhenNoValidEndpoints() {
        Ring ring = Ring.make(CoreHelper.<Endpoint>asSet());
        assertNotNull(ring.uuid());
    }

    private long toLong(byte[] values) {
        long shift = 0;
        long total = 0;
        for (byte value : values) {
            final long lval = (int)(value&0xff);
            total = total + lval << shift;
            shift += 8;
        }
        
        return total;
    }
}
