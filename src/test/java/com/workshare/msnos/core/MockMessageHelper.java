package com.workshare.msnos.core;

import com.workshare.msnos.core.payloads.NullPayload;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by rhys on 04/08/14.
 */
public class MockMessageHelper {

    private final Message message;
    private final Message.Type type;
    private final Iden from;
    private final Iden to;

    private Message.Payload payload;
    private String sig;
    private String rnd;
    private long seq;

    public MockMessageHelper(Message.Type type, Iden from, Iden to) {
        this.type = type;
        this.from = from;
        this.to = to;
        message = mock(Message.class);
    }

    public MockMessageHelper data(Message.Payload payload) {
        this.payload = payload;
        return this;
    }

    public MockMessageHelper signed(String sig, String rnd) {
        this.sig = sig;
        this.rnd = rnd;
        return this;
    }

    public MockMessageHelper sequence(long seq) {
        this.seq = seq;
        return this;
    }

    public Message make() {
        when(message.getSig()).thenReturn(sig);
        when(message.getRnd()).thenReturn(rnd);
        when(message.getSeq()).thenReturn(seq);
        when(message.getType()).thenReturn(type);
        when(message.getFrom()).thenReturn(from);
        when(message.getTo()).thenReturn(to);
        if (payload == null) when(message.getData()).thenReturn(new NullPayload());
        else when(message.getData()).thenReturn(payload);
        return message;
    }
}
