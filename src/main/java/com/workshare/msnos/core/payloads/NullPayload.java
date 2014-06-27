package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Message.Payload;

public class NullPayload extends PayloadAdapter implements Payload  {

    public static final NullPayload INSTANCE = new NullPayload();

}
