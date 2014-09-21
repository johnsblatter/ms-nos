package com.workshare.msnos.usvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.usvc.api.RestApi;

public class PassiveServiceTest {

    private Microcloud microcloud;
    private PassiveService passiveService;

    @Before
    public void prepare() {
        microcloud = mock(Microcloud.class);
        passiveService = new PassiveService(microcloud, "testPassive", "10.10.10.10", 9999, "http://10.10.10.10/healthcheck/");
    }
    
    @Test
    public void shouldInvokeCloudOnJoin() throws Exception {
        passiveService.join();

        verify(microcloud, times(1)).onJoin(passiveService);
    }

    @Test
    public void shouldInvokeCloudOnPublish() throws Exception {
        passiveService.join();

        RestApi[] apis = new RestApi[]{mock(RestApi.class), mock(RestApi.class)};
        passiveService.publish(apis);
        
        verify(microcloud, times(1)).publish(passiveService, apis);
    }
}