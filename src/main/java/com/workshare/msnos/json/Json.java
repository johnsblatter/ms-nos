package com.workshare.msnos.json;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

import com.google.gson.JsonElement;

public class Json {
    private static final ThreadSafeGson gson = new ThreadSafeGson();

    public static final String toJsonString(Object anyObject) {
        return gson.toJson(anyObject);
    }

    public static final <T> T fromJsonString(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
 
    public static final <T> T fromBytes(byte[] array, Class<T> clazz) {
        return fromJsonString(new String(array, Charset.forName("UTF-8")), clazz);
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

}
