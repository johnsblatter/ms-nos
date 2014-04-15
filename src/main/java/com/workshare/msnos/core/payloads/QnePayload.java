package com.workshare.msnos.core.payloads;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.usvc.RestApi;

public class QnePayload implements Payload {
    private Set<RestApi> apis;
    
    public QnePayload(RestApi... apis) {
        this.apis = Collections.unmodifiableSet(new HashSet<RestApi>(Arrays.asList(apis)));
    }

    public Set<RestApi> getApis() {
        return apis;
    }
}
