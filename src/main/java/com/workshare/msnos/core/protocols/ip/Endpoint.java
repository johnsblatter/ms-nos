package com.workshare.msnos.core.protocols.ip;

import com.workshare.msnos.soup.json.Json;

import java.net.InetAddress;

public class Endpoint {

    private final InetAddress host;
    private final short port;

    public Endpoint(Network network, InetAddress host, short port) {
        this.host = host;
        this.port = port;
    }

    public InetAddress getHost() {
        return host;
    }

    public short getPort() {
        return port;
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }
}
