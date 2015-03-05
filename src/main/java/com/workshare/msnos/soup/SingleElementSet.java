package com.workshare.msnos.soup;

import java.util.AbstractSet;
import java.util.Iterator;

public class SingleElementSet<T> extends AbstractSet<T> {

    private T item;

    public SingleElementSet() {
        this(null);
    }

    public SingleElementSet(T item) {
        this.item = item;
    }

    @Override
    public boolean add(T elem) {
        item = elem;
        return true;
    }

    @Override
    public int size() {
        return (item == null ? 0 : 1);
    }

    @Override
    public Iterator<T> iterator() {
        return new SingleItemIterator<T>(item);
    }

}
