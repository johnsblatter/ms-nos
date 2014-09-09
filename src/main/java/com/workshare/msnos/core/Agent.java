package com.workshare.msnos.core;

import java.util.Set;

import com.workshare.msnos.core.protocols.ip.Endpoint;

public interface Agent extends Identifiable {
    Iden getIden();

    Cloud getCloud();

    Set<Endpoint> getEndpoints();

    Long getSeq();

    long getAccessTime();

    void touch();
}
