package com.workshare.msnos.core.cloud;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Iden;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class AgentsList<T extends Agent> {

    private ConcurrentHashMap<Iden, T> agents;

    public AgentsList() {
        this.agents = new ConcurrentHashMap<Iden, T>();
    }

    public Collection<T> list() {
        return Collections.unmodifiableCollection(agents.values());
    }

    public boolean containsKey(Iden iden) {
        return agents.containsKey(iden);
    }

    public void add(T agent) {
        agents.put(agent.getIden(), agent);
    }

    public T remove(Iden iden) {
        return agents.remove(iden);
    }

    public T touch(Iden iden) {
        if (agents.containsKey(iden)) {
            T agent = agents.get(iden);
            agent.touch();
            return agent;
        } else
            return null;
    }
}
