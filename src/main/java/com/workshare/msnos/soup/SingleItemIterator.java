package com.workshare.msnos.soup;
import java.util.Iterator;

public class SingleItemIterator<T> implements Iterator<T> {

    private T value;

    public SingleItemIterator(final T value) {
        this.value = value;
    }
    
    @Override
    public boolean hasNext() {
        return value != null;
    }

    @Override
    public T next() {
        T res = value;
        value = null;
        return res;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removal is not supported here!");
    }

}
