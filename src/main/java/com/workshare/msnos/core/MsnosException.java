package com.workshare.msnos.core;

import java.io.IOException;

@SuppressWarnings("serial")
public class MsnosException extends IOException {

    public enum Code {
        TRANSPORT_ERROR, 
        NOT_CONNECTED, 
        JOIN_FAILED, 
        SEND_FAILED, 
        INVALID_STATE
    }

    private final Code code;
    
    public MsnosException(IOException cause) {
        super(cause);
        this.code = Code.TRANSPORT_ERROR;
    }
    
    public MsnosException(String message, IOException cause) {
        this(message, cause, Code.TRANSPORT_ERROR);
    }

    public MsnosException(String message, IOException cause, Code code) {
        super(message, cause);
        this.code = code;
    }

    public MsnosException(String message, Code code) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
