package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;

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