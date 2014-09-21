package com.workshare.msnos.core;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.workshare.msnos.core.protocols.ip.Endpoint;

public class PassiveAgent extends RemoteEntity implements Agent {

    public PassiveAgent(Cloud cloud, UUID uuid) {
        super(new Iden(Iden.Type.AGT, uuid), cloud);
    }

    @Override
    public Set<Endpoint> getEndpoints() {
        return Collections.<Endpoint>emptySet() ;
    }

    @Override
    public boolean equals(Object other) {
        try {
            return this.getIden().equals(((Agent) (other)).getIden());
        } catch (Exception any) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getIden().hashCode();
    }
}
