package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.usvc.RestApi;

import java.util.*;

public class QnePayload implements Payload {
    private Set<RestApi> apis;

    public QnePayload(RestApi... apis) {
        this.apis = Collections.unmodifiableSet(new HashSet<RestApi>(Arrays.asList(apis)));
    }

    public Set<RestApi> getApis() {
        return apis;
    }

    //    TODO FIXME
    @Override
    public Payload[] split() {
        return null;
    }
}
