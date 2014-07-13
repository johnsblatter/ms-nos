package com.workshare.msnos.soup;

import java.util.AbstractList;

public class SingleElementList<T> extends AbstractList<T> {

    private T item;

    public SingleElementList() {
        this(null);
    }

    public SingleElementList(T item) {
        this.item = item;
    }

    @Override
    public boolean add(T elem) {
        item = elem;
        return true;
    }

    @Override
    public T get(int index) {
        return index == 0 ? item : null;
    }

    @Override
    public int size() {
        return (item == null ? 0 : 1);
    }

}
