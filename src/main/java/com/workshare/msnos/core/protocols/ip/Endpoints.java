package com.workshare.msnos.core.protocols.ip;

import java.util.Set;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.MsnosException;

public interface Endpoints {

    public Set<? extends Endpoint> all();

    public Set<? extends Endpoint> publics();

    public Set<? extends Endpoint> of(Agent agent);
    
    public Endpoint install(Endpoint endpoint) throws MsnosException;

    public Endpoint remove(Endpoint endpoint) throws MsnosException;
}
