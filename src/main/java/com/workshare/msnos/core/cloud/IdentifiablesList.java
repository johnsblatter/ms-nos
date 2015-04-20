package com.workshare.msnos.core.cloud;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Identifiable;

public class IdentifiablesList<T extends Identifiable> {

    private final ConcurrentHashMap<Iden, T> entities;

    public IdentifiablesList() {
        this.entities = new ConcurrentHashMap<Iden, T>();
    }

    public Collection<T> list() {
        return Collections.unmodifiableCollection(entities.values());
    }

    public boolean containsKey(Iden iden) {
        return entities.containsKey(iden);
    }

    public void add(T agent) {
        entities.put(agent.getIden(), agent);
    }

    public T remove(Iden iden) {
        return entities.remove(iden);
    }

    public T get(Iden iden) {
        return entities.get(iden);
    }
    
    public String toString() {
        return this.entities.toString();
    }
}
