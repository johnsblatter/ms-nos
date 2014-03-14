package com.workshare.msnos.protocols.ip;

import java.io.IOException;
import java.net.MulticastSocket;

public class MulticastSocketFactory {

    public MulticastSocket create() throws IOException {
        return new MulticastSocket();
    }
}
