package com.workshare.msnos.core;

import com.google.gson.JsonObject;

class Messages {

    static JsonObject STATUS_TRUE;

    static {
        STATUS_TRUE = new JsonObject();
        STATUS_TRUE.addProperty("status", true);
    }

    static JsonObject STATUS_FALSE;

    static {
        STATUS_FALSE = new JsonObject();
        STATUS_FALSE.addProperty("status", false);
    }

    public static Message presence(Identifiable from, Identifiable to) {
        return new Message(Message.Type.PRS, from.getIden(), to.getIden(), 2, false, STATUS_TRUE);
    }

    public static Message absence(Identifiable from, Identifiable to) {
        return new Message(Message.Type.PRS, from.getIden(), to.getIden(), 2, false, STATUS_FALSE);
    }

    public static Message app(Identifiable from, Identifiable to, JsonObject data) {
        return new Message(Message.Type.APP, from.getIden(), to.getIden(), 2, false, data);
    }

    public static Message discovery(Identifiable from, Identifiable to) {
        return new Message(Message.Type.DSC, from.getIden(), to.getIden(), 2, false, null);
    }

}
