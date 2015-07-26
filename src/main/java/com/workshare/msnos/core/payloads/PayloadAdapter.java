package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.cloud.Cloud.Internal;

public class PayloadAdapter implements Payload {

    public PayloadAdapter() {
        super();
    }

    @Override
    public Payload[] split() {
        return new Payload[]{this};
    }

    @Override
    public boolean process(Message message, Internal internal) {
       return false;
    }

}