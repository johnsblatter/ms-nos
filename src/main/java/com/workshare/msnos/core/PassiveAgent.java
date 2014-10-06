package com.workshare.msnos.core;

import com.workshare.msnos.core.protocols.ip.Endpoint;

import java.util.Set;
import java.util.UUID;

public class PassiveAgent extends RemoteEntity implements Agent {

    public PassiveAgent(Cloud cloud, UUID uuid) {
        super(new Iden(Iden.Type.AGT, uuid), cloud);
    }

    @Override
    public Set<Endpoint> getEndpoints() {
        return null;
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
