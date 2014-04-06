package com.workshare.msnos.soup.json;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;

public class Json {
    private static final ThreadSafeGson gson = new ThreadSafeGson();

    public static final String toJsonString(Object anyObject) {
        return gson.toJson(anyObject);
    }

    public static final <T> T fromJsonString(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
 
    public static final <T> T fromJsonTree(JsonElement json, Type typeOfT)  {
        return gson.fromJsonTree(json, typeOfT);
    }
}
