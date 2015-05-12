package com.workshare.msnos.core;

import java.util.Set;

import com.workshare.msnos.core.protocols.ip.Endpoint;

/**
 * An interface that represents 
 * an agent in a cloud
 * 
 * @author bbossola
 * @see Cloud
 */
public interface Agent extends Identifiable {
    
    /* (non-Javadoc)
     * @see com.workshare.msnos.core.Identifiable#getIden()
     */
    Iden getIden();

    /**
     * Returns the cloud this agent is connected to
     * 
     * @return  the connected cloud or null if not connected
     */
    Cloud getCloud();

    /**
     * Returns the endpoints exposed by this agent
     * 
     * @return  the list of such endpoints
     */
    Set<Endpoint> getEndpoints();

    /**
     * Returns the last time this access was seen
     * 
     * @return  last time seen
     */
    long getAccessTime();

    /**
     * Allows to "touch" an agent, resetting his access time to now
     */
    void touch();

    /**
     * Returns the ring this agent is connected to
     * 
     * @return  the ring who's this agent is connected to
     */
    Ring getRing();
}
