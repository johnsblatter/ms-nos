package com.workshare.msnos.core;

import com.workshare.msnos.core.protocols.ip.Network;

import java.util.Set;

public interface AgentInterface extends Identifiable {
    Iden getIden();

    Cloud getCloud();

    long getAccessTime();

    Set<Network> getHosts();
}
