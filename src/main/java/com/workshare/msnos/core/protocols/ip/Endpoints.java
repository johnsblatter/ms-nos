package com.workshare.msnos.core.protocols.ip;

import java.util.Set;

import com.workshare.msnos.core.MsnosException;

public interface Endpoints {

    public Set<? extends Endpoint> all();
    
    public Endpoint install(Endpoint endpoint) throws MsnosException;

    public Endpoint remove(Endpoint endpoint) throws MsnosException;
}
