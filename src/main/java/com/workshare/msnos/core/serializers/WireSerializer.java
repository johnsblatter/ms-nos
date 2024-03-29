package com.workshare.msnos.core.serializers;

import java.io.Reader;

public interface WireSerializer {

    public <T> T fromText(String text, Class<T> clazz);

    public String toText(Object anyObject);

    public <T> T fromBytes(byte[] array, Class<T> clazz);

    public <T> T fromBytes(byte[] array, int offset, int length, Class<T> clazz);

    public byte[] toBytes(Object anyObject);

    public <T> T fromReader(Reader reader, Class<T> clazz);
}
