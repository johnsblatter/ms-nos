package com.workshare.msnos.protocols.ip.udp;

import java.util.UUID;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;

// an horrible static class to host utility methods
// to share across tests
//
// FIXME remove this crap :)
//
public class Utils {
    public static Message newSampleMessage() {
        final UUID uuid = new UUID(123, 456);
        final Iden src = new Iden(Iden.Type.AGT, uuid);
        final Iden dst = new Iden(Iden.Type.CLD, uuid);
        final Message message = new Message(Message.Type.APP, src, dst, "sigval", 1, false, null);
        return message;
    }
}
