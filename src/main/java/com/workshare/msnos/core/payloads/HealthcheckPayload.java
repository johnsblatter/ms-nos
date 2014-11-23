package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Iden;

public class HealthcheckPayload extends PayloadAdapter {

    private final Iden agent;
    private final boolean working;

    public HealthcheckPayload(Agent agent, boolean working) {
        if (agent == null)
            throw new IllegalArgumentException("Agent cannot be null!");
            
        this.agent = agent.getIden();
        this.working = working;
    }
    
    public Iden getIden() {
        return agent;
    }
    
    public boolean isWorking() {
        return working;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HealthcheckPayload that = (HealthcheckPayload) o;

        return this.agent.equals(that.agent) && this.working == that.working;
    }

    @Override
    public int hashCode() {
        return agent.hashCode();
    }

    
}
