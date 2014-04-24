package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.usvc.RestApi;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class QnePayload implements Payload {
    private Set<RestApi> apis;

    private String name;

    public QnePayload(String name, RestApi... apis) {
        this.name = name;
        this.apis = Collections.unmodifiableSet(new HashSet<RestApi>(Arrays.asList(apis)));
    }

    public Set<RestApi> getApis() {
        return apis;
    }

    public String getName() {
        return name;
    }

    @Override
    public Payload[] split() {
        return null;
    }
}
