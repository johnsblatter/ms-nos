package com.workshare.msnos.core.cloud;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Identifiable;

public class IdentifiablesList<T extends Identifiable> {

    public interface Callback<T> {
        public void onAdd(T identifiable)
        ;

        public void onRemove(T identifiable)
        ;
    }
    
    @SuppressWarnings("hiding")
    public final class CallbackAdapter<T> implements Callback<T> {
        @Override
        public void onAdd(T identifiable) {
        }
        @Override
        public void onRemove(T identifiable) {
        }
    }
    
    private final Callback<T> callback;
    private final ConcurrentHashMap<Iden, T> entities;

    public IdentifiablesList() {
        this(null);
    }

    public IdentifiablesList(Callback<T> callback) {
        this.entities = new ConcurrentHashMap<Iden, T>();
        this.callback = (callback != null ? callback : new CallbackAdapter<T>());
    }

    public Collection<T> list() {
        return Collections.unmodifiableCollection(entities.values());
    }

    public boolean containsKey(Iden iden) {
        return entities.containsKey(iden);
    }

    public void add(T agent) {
        entities.put(agent.getIden(), agent);
        callback.onAdd(agent);
    }

    public T remove(Iden iden) {
        final T target = entities.remove(iden);
        if (target != null)
            callback.onRemove(target);
        return target;
    }

    public T get(Iden iden) {
        return entities.get(iden);
    }
    
    public String toString() {
        return this.entities.toString();
    }
}
