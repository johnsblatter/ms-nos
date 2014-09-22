package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Cloud;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class PassiveServiceTest {

    PassiveService passiveService;
    Microservice microservice;

    @Test
    public void shouldInvokeMicroserviceOnJoin() throws Exception {
        microservice = spy(new Microservice("test"));
        microservice.join(new Cloud(new UUID(111, 222)));

        passiveService = new PassiveService(microservice, new UUID(111, 222), "testPassive", "10.10.10.10", "http://10.10.10.10/healthcheck/", 9999);
        passiveService.join();

        verify(microservice, times(1)).passiveJoin(passiveService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNOTAllowCreationIfCloudUUIDDifferent() throws Exception {
        Cloud cloud = new Cloud(new UUID(111, 222));
        microservice = new Microservice("test");
        microservice.join(cloud);

        passiveService = new PassiveService(microservice, new UUID(456, 789), "test", "test", "test", 9999);
        assertEquals(null, passiveService);
    }


}