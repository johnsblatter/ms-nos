package com.workshare.msnos.core.serializers;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.Version;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.json.ThreadSafeGson;

public class WireJsonSerializer implements WireSerializer {

    private static Logger log = LoggerFactory.getLogger(WireSerializer.class);

    @Override
    public <T> T fromText(String text, Class<T> clazz) {
        try {
            return gson.fromJson(text, clazz);
        }
        catch (JsonSyntaxException ex) {
            log.warn("Error parsing JSON content: {}", text);
            throw ex;
        }
    }

    @Override
    public String toText(Object anyObject) {
        return gson.toJson(anyObject);
    }

    @Override
    public <T> T fromBytes(byte[] array, Class<T> clazz) {
        return fromText(new String(array, Charset.forName("UTF-8")), clazz);
    }

    @Override
    public <T> T fromBytes(byte[] array, int offset, int length, Class<T> clazz) {
        return fromText(new String(array, offset, length, Charset.forName("UTF-8")), clazz);
    }

    @Override
    public byte[] toBytes(Object anyObject) {
        return gson.toJson(anyObject).getBytes(Charset.forName("UTF-8"));
    }

    private static final JsonSerializer<Iden> ENC_IDEN = new JsonSerializer<Iden>() {
        @Override
        public JsonElement serialize(Iden iden, Type typeof, JsonSerializationContext context) {
            return serializeIden(iden);
        }
    };

    private static final JsonDeserializer<Iden> DEC_IDEN = new JsonDeserializer<Iden>() {
        @Override
        public Iden deserialize(JsonElement json, Type typeof, JsonDeserializationContext context) throws JsonParseException {
            return deserializeIden(json);
        }
    };

    private static final JsonSerializer<Cloud> ENC_CLOUD = new JsonSerializer<Cloud>() {
        @Override
        public JsonElement serialize(Cloud cloud, Type typeof, JsonSerializationContext context) {
            return serializeIden(cloud.getIden());
        }
    };

    private static final JsonDeserializer<Cloud> DEC_CLOUD = new JsonDeserializer<Cloud>() {
        @Override
        public Cloud deserialize(JsonElement json, Type typeof, JsonDeserializationContext context) throws JsonParseException {
            try {
                Iden iden = deserializeIden(json);
                if (iden.getType() != Iden.Type.CLD)
                    throw new IllegalArgumentException("Unexpected type when converting cloud!");
                return new Cloud(iden.getUUID(), Collections.<Gateway> emptySet());
            } catch (Exception any) {
                throw new JsonParseException(any);
            }

        }
    };

    private static final JsonSerializer<Agent> ENC_AGENT = new JsonSerializer<Agent>() {
        @Override
        public JsonElement serialize(Agent agent, Type typeof, JsonSerializationContext context) {
            return serializeIden(agent.getIden());
        }
    };

    private static final JsonDeserializer<Agent> DEC_AGENT = new JsonDeserializer<Agent>() {
        @Override
        public Agent deserialize(JsonElement json, Type typeof, JsonDeserializationContext context) throws JsonParseException {
            Iden iden = deserializeIden(json);
            if (iden.getType() != Iden.Type.AGT)
                throw new IllegalArgumentException("Unexpected type when converting cloud!");
            return new Agent(iden.getUUID());
        }
    };

    private static final JsonSerializer<Version> ENC_VERSION = new JsonSerializer<Version>() {
        @Override
        public JsonElement serialize(Version src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getMajor() + "." + src.getMinor());
        }
    };

    private static final JsonDeserializer<Version> DEC_VERSION = new JsonDeserializer<Version>() {
        @Override
        public Version deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final String text = json.getAsString();
            final int dotIndex = text.indexOf(".");

            final int major = Integer.parseInt(text.substring(0, dotIndex));
            final int minor = Integer.parseInt(text.substring(dotIndex + 1));

            return new Version(major, minor);
        }
    };

    private static final JsonSerializer<Message> ENC_MESSAGE = new JsonSerializer<Message>() {
        @Override
        public JsonElement serialize(Message msg, Type typeof, JsonSerializationContext context) {
            final JsonObject res = new JsonObject();
            res.add("v", context.serialize(msg.getVersion()));
            res.add("fr", context.serialize(msg.getFrom()));
            res.add("to", context.serialize(msg.getTo()));
            res.addProperty("hp", msg.getHops());
            res.addProperty("rx", msg.isReliable());
            final Message.Type msgType = msg.getType();
            res.addProperty("ty", msgType.toString());
            res.add("dt", context.serialize(msg.getData()));
            return res;
        }
    };

    private static final JsonDeserializer<Message> DEC_MESSAGE = new JsonDeserializer<Message>() {
        @Override
        public Message deserialize(JsonElement json, Type typeof, JsonDeserializationContext context) throws JsonParseException {

            final JsonObject obj = json.getAsJsonObject();
            final Message.Type type = Message.Type.valueOf(obj.get("ty").getAsString());
            final Iden from = context.deserialize(obj.get("fr").getAsJsonPrimitive(), Iden.class);
            final Iden to = context.deserialize(obj.get("to").getAsJsonPrimitive(), Iden.class);
            final int hops = obj.get("hp").getAsInt();
            final boolean reliable = obj.get("hp").getAsBoolean();

            Payload data = null;
            JsonElement dataJson = obj.get("dt");
            if (dataJson != null) {
                switch (type) {
                    case PRS: data = (Payload)Json.fromJsonTree(dataJson, Presence.class);
                        break;
                    default:
                        break;
                }
            }

            return new Message(type, from, to, hops, reliable, data);
        }
    };

    private static final ThreadSafeGson gson = new ThreadSafeGson() {
        protected Gson newGson() {
            GsonBuilder builder = new GsonBuilder();

            builder.registerTypeAdapter(Cloud.class, ENC_CLOUD);
            builder.registerTypeAdapter(Cloud.class, DEC_CLOUD);

            builder.registerTypeAdapter(Agent.class, ENC_AGENT);
            builder.registerTypeAdapter(Agent.class, DEC_AGENT);

            builder.registerTypeAdapter(Iden.class, ENC_IDEN);
            builder.registerTypeAdapter(Iden.class, DEC_IDEN);

            builder.registerTypeAdapter(Version.class, ENC_VERSION);
            builder.registerTypeAdapter(Version.class, DEC_VERSION);

            builder.registerTypeAdapter(Message.class, ENC_MESSAGE);
            builder.registerTypeAdapter(Message.class, DEC_MESSAGE);

            return builder.create();
        }
    };

    private static final JsonPrimitive serializeIden(Iden iden) {
        return new JsonPrimitive(iden.getType() + ":" + iden.getUUID());
    }

    private static final Iden deserializeIden(JsonElement json) {
        try {
            String text = json.getAsString();
            int idx = text.indexOf(':');

            Iden.Type type = Iden.Type.valueOf(text.substring(0, idx));
            UUID uuid = UUID.fromString(text.substring(idx + 1));
            return new Iden(type, uuid);
        } catch (Exception any) {
            throw new JsonParseException(any);
        }
    }
}
