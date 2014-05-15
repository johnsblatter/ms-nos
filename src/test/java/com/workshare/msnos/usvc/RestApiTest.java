package com.workshare.msnos.usvc;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class RestApiTest {

    RestApi restApi;

    @Test
    public void shouldBuildRestApiWithHostWhenSpecified() {
        restApi = new RestApi("/test", 9999).host("10.10.1.1");
        assertEquals(restApi.getHost(), "10.10.1.1");
    }

}
