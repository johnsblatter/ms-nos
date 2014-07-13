package com.workshare.msnos.soup;

import java.util.AbstractQueue;
import java.util.Iterator;

public class SingleElementQueue<T> extends AbstractQueue<T> {

    private T item;

    public SingleElementQueue() {
        this(null);
    }

    public SingleElementQueue(T item) {
        this.item = item;
    }

    @Override
    public boolean offer(T e) {
        item = e ;
        return true;
    }

    @Override
    public T peek() {
        return item;
    }

    @Override
    public T poll() {
        T res = item;
        item = null;
        return res;
    }

    @Override
    public Iterator<T> iterator() {
        final Object[] pointer = new Object[1];
        pointer[0] = item;
        
        return new Iterator<T>() {

            @Override
            public boolean hasNext() {
                return pointer[0] == null ? null : true;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                T res = (T)pointer[0];
                pointer[0] = null;
                return res;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }};
    }

    @Override
    public int size() {
        return (item == null ? 0 : 1);
    }
}