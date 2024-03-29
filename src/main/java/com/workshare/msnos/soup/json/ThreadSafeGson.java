package com.workshare.msnos.soup.json;

import java.io.Reader;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.LocalAgent;

public class ThreadSafeGson {
    private final ThreadLocal<Gson> threadSafeGson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return newGson();
        }
    };

    protected Gson newGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Cloud.class, new JsonSerializer<Cloud>() {
            @Override
            public JsonElement serialize(Cloud src, Type typeOfSrc, JsonSerializationContext context) {
                final JsonObject res = new JsonObject();
                res.add("iden", context.serialize(src.getIden()));

                final JsonArray idens = new JsonArray();
                for (final LocalAgent agent : src.getLocalAgents()) {
                    idens.add(context.serialize(agent.getIden()));
                }
                res.add("agents", idens);
                ;

                return res;
            }
        });

        builder.registerTypeAdapter(Byte.class, new JsonSerializer<Byte>() {
            @Override
            public JsonElement serialize(Byte src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive((int)(src&0xff));
            }});

        return builder.create();
    }

    public final String toJson(Object anyObject) {
        return gson().toJson(anyObject);
    }

    public final <T> T fromJson(String json, Class<T> clazz) {
        return gson().fromJson(json, clazz);
    }

    public final JsonElement toJsonTree(Object src, Type typeOfSrc) {
        return gson().toJsonTree(src, typeOfSrc);
    }

    public final <T> T fromJsonTree(JsonElement json, Type typeOfT) {
        return gson().fromJson(json, typeOfT);
    }

    public final <T> T fromReader(Reader reader, Type typeOfT) {
        return gson().fromJson(reader, typeOfT);
    }

    public Gson gson() {
        return threadSafeGson.get();
    }

}
