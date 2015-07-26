package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.cloud.Cloud.Internal;
import com.workshare.msnos.core.services.api.RestApi;
import com.workshare.msnos.soup.json.Json;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class  QnePayload implements Payload {
    
    private String name;
    private Set<RestApi> apis;

    public QnePayload(String name, RestApi... apis) {
        this(name,  Collections.unmodifiableSet(new HashSet<RestApi>(Arrays.asList(apis))));
    }

    public QnePayload(String name, Set<RestApi> apis) {
        this.name = name;
        this.apis = apis;
    }

    public Set<RestApi> getApis() {
        return apis;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }


    @Override
    public Payload[] split() {

        Set<RestApi> apisOne = new HashSet<RestApi>();
        Set<RestApi> apisTwo = new HashSet<RestApi>();

        int i = 0;
        for (RestApi api : apis) {
            if (i++%2 == 0)
                apisOne.add(api);
            else
                apisTwo.add(api);
        }
        
        
        return new Payload[] {
            new QnePayload(name, apisOne),
            new QnePayload(name, apisTwo)
        };
    }

    @Override
    public boolean process(Message message, Internal internal) {
        return false;
    }
}
