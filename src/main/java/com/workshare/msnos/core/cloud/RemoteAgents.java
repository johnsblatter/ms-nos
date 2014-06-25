package com.workshare.msnos.core.cloud;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.RemoteAgent;

public class RemoteAgents {

    private ConcurrentHashMap<Iden, RemoteAgent> remoteAgents;

    public RemoteAgents() {
        this.remoteAgents = new ConcurrentHashMap<Iden, RemoteAgent>();
    }
    
    public Collection<RemoteAgent> list() {
        return Collections.unmodifiableCollection(remoteAgents.values());
    }

    public boolean containsKey(Iden iden) {
        return remoteAgents.containsKey(iden);
    }

    public void add(RemoteAgent agent) {
        remoteAgents.put(agent.getIden(), agent);
    }

    public RemoteAgent remove(Iden iden) {
        return remoteAgents.remove(iden);
    }

    public RemoteAgent touch(Iden iden) {
        if (remoteAgents.containsKey(iden)) {
            RemoteAgent agent = remoteAgents.get(iden);
            agent.touch();
            return agent;
        } else
            return null;
    }

}
