package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.cloud.Cloud.Internal;

public class PongPayload implements Message.Payload {

    @Override
    public Payload[] split() {
        return new Payload[]{this};
    }

    @Override
    public boolean process(Message message, Internal internal) {
        return false;
    }

    @Override
    public int hashCode() {
        return "pong".hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PongPayload;
    }

    
}
