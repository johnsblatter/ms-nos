package com.workshare.msnos.core.payloads;

import com.google.gson.JsonElement;
import com.workshare.msnos.core.Message.Payload;

public class GenericPayload extends PayloadAdapter implements Payload {

    private final JsonElement data;

    public GenericPayload(JsonElement data) {
        this.data = data;
    }

    public JsonElement getData() {
        return data;
    }
}
