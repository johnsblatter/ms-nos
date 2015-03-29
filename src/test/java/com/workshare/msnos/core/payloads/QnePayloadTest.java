package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.usvc.api.RestApi;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class QnePayloadTest {

    @Test
    public void shouldSplitApisCollectionOnSplit() {
        final RestApi alfa = newApi("alfa");
        final RestApi beta = newApi("beta");
        QnePayload payload = new QnePayload("foo", alfa, beta);

        Payload[] loads = payload.split();
        assertEquals(2, loads.length);

        Set<RestApi> newApis = new HashSet<RestApi>();
        newApis.add(getApi(loads[0]));
        newApis.add(getApi(loads[1]));
        assertEquals(payload.getApis(), newApis);
    }

    @Test
    public void shouldPreserveServiceNameOnSplit() {
        QnePayload payload = new QnePayload("foo", newApi("alfa"), newApi("beta"));

        Payload[] loads = payload.split();
        for (Payload load : loads) {
            assertEquals("foo", ((QnePayload) load).getName());
        }
    }

    private RestApi newApi(final String path) {
        return new RestApi(path, (int) Math.random() * 1000);
    }

    private RestApi getApi(Payload payload) {
        QnePayload load = (QnePayload) payload;
        Set<RestApi> apis = load.getApis();
        if (apis.size() == 1)
            return apis.iterator().next();
        else
            throw new AssertionError("One API only was expected!");
    }


}
