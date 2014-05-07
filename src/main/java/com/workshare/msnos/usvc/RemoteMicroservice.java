package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Iden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class RemoteMicroservice {

    private static final Logger log = LoggerFactory.getLogger(RemoteMicroservice.class);

    private Iden iden;
    private String name;
    private Set<RestApi> apis;

    public RemoteMicroservice(Iden iden, String name, Set<RestApi> apis) {
        this.iden = iden;
        this.name = name;
        this.apis = apis;
    }

    public Iden getIden() {
        return iden;
    }

    public String getName() {
        return name;
    }

    public Set<RestApi> getApis() {
        return apis;
    }

}
