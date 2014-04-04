package com.workshare.msnos.soup.json;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import com.google.gson.JsonElement;
import com.workshare.msnos.core.Message;

public class Json {
    private static final ThreadSafeGson gson = new ThreadSafeGson();
    private static final boolean DUMP = false;

    public static final String toJsonString(Object anyObject) {
        return gson.toJson(anyObject);
    }

    public static final <T> T fromJsonString(String json, Class<T> clazz) {
        System.out.println(json);
        return gson.fromJson(json, clazz);
    }
 
    public static final <T> T fromBytes(byte[] array, Class<T> clazz) {
        if (DUMP) dumpBuffer(array, 0, array.length);
        return fromJsonString(new String(array, Charset.forName("UTF-8")), clazz);
    }

    public static final <T> T fromBytes(byte[] array, int offset, int length, Class<T> clazz) {
        if (DUMP) dumpBuffer(array, offset, length);
        return fromJsonString(new String(array, offset, length, Charset.forName("UTF-8")), clazz);
    }

    public static final JsonElement toJsonTree(Object src, Type typeOfSrc)  {
        return gson.toJsonTree(src, typeOfSrc);
    }
    
    public static final <T> T fromJsonTree(JsonElement json, Type typeOfT)  {
        return gson.fromJsonTree(json, typeOfT);
    }

    public static byte[] toBytes(Object anyObject) {
        return gson.toJson(anyObject).getBytes(Charset.forName("UTF-8"));
    }

    public static void main(String[] args) {
        String s = "{\"version\":{\"major\":1,\"minor\":0},\"type\":\"APP\",\"from\":{\"type\":\"AGT\",\"uuid\":\"c2ec3b72-10d9-47d7-88ec-0313b0e885ef\"},\"to\":{\"type\":\"CLD\",\"uuid\":\"00000000-0000-007b-0000-0000000001c8\"},\"sig\":\"sigval\",\"hops\":1,\"reliable\":false}";
        System.out.println(gson.fromJson(s, Message.class));
    }
    
    private static void dumpBuffer(byte[] array, int offset, int length) {
        try {
            FileOutputStream fos = new FileOutputStream("/tmp/tmp.bin");
            try {
                fos.write(array, offset, length);
            }
            finally {
                fos.close();
            }

            final String json = new String(array, offset, length, Charset.forName("UTF-8"));
            FileWriter out = new FileWriter("/tmp/tmp.txt");
            try {
                out.write(json);
            }
            finally {
                out.close();
            }
            
        } catch (Exception ignore) {}
    }
    
}
