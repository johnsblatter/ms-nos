package com.workshare.msnos.core;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.workshare.msnos.json.Json;

public class Version {

    public static final Version V1_0 = new Version(1,0);
    
    private final int major;
    private final int minor;
    
    public Version(int major, int minor) {
        super();
        this.major = major;
        this.minor = minor;
    }
    
    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }
   
    public static class Serializer implements JsonSerializer<Version> {
        @Override
        public JsonElement serialize(Version src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.major+"."+src.minor);
        }
    }

    public static class Deserializer implements JsonDeserializer<Version> {
        @Override
        public Version deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final String text = json.getAsString();
            final int dotIndex = text.indexOf(".");

            final int major = Integer.parseInt(text.substring(0, dotIndex));
            final int minor = Integer.parseInt(text.substring(dotIndex+1));

            return new Version(major, minor);
        }
    }
    
    public String toString() {
        return Json.toJsonString(this);
    }

}
