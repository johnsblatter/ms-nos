package com.workshare.msnos.json;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class ThreadSafeGson {
    private final ThreadLocal<Gson> threadSafeGson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return newGson();
        }
    };

    protected Gson newGson() {
        return new Gson();
    }

    public final String toJson(Object anyObject) {
        return gson().toJson(anyObject);
    }

    public final <T> T fromJson(String json, Class<T> clazz) {
        return gson().fromJson(json, clazz);
    }

    public final JsonElement toJsonTree(Object src, Type typeOfSrc)  {
        return gson().toJsonTree(src, typeOfSrc);
    }
    
    public final <T> T fromJsonTree(JsonElement json, Type typeOfT)  {
        return gson().fromJson(json, typeOfT);
    }
    
    public Gson gson() {
        return threadSafeGson.get();
    }

}
