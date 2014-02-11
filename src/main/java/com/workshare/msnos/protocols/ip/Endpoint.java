package com.workshare.msnos.protocols.ip;

import java.net.InetAddress;

import com.workshare.msnos.json.Json;

public class Endpoint {
    private final Network network;
    private final InetAddress host;
    private final short port;
    
    public Endpoint(Network network, InetAddress host, short port) {
        super();
        this.network = network;
        this.host = host;
        this.port = port;
    }

    public Network getNetwork() {
        return network;
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
