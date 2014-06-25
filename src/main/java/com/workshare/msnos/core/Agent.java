package com.workshare.msnos.core;

import com.workshare.msnos.core.protocols.ip.Network;

import java.util.Set;

public interface Agent extends Identifiable {
    Iden getIden();

    Cloud getCloud();

    Set<Network> getHosts();

    long getAccessTime();

    void touch();
}
