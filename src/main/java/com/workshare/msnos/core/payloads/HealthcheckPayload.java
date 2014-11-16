package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Iden;

public class HealthcheckPayload extends PayloadAdapter {

    private final Iden agent;
    private final boolean working;

    public HealthcheckPayload(Agent agent, boolean working) {
        this.agent = agent.getIden();
        this.working = working;
    }
    
    public Iden getIden() {
        return agent;
    }
    
    public boolean isWorking() {
        return working;
    }

}
