package com.workshare.msnos.core.protocols.ip;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.MsnosException.Code;
import com.workshare.msnos.soup.json.Json;

public class Endpoint {

    public enum Type {
        UDP(0), HTTP(80), SSH(22);

        private short defaultPort;

        Type(int defaultPort) {
            this.defaultPort = (short) defaultPort;
        }

        public short defaultPort() {
            return defaultPort;
        }
    }

    private final Type type;
    private final Network network;
    private final short port;

    public Endpoint(Type type, Network host) {
        this(type, host, type.defaultPort());
    }

    public Endpoint(Type type, Network host, short port) {
        if (type == null || host == null)
            throw new IllegalArgumentException("No argument can be null here");

        this.type = type;
        this.network = host;
        this.port = port;
    }

    public Type getType() {
        return type;
    }

    public Network getNetwork() {
        return network;
    }

    public short getPort() {
        return port;
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((network == null) ? 0 : network.hashCode());
        result = prime * result + port;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Endpoint other = (Endpoint) obj;
            return network.equals(other.network) && type == other.type && port == other.port;
        } catch (Exception ignore) {
            return false;
        }
    }

    public static Endpoints create(final Endpoint... points) {
        return create(new HashSet<Endpoint>(Arrays.asList(points)));
    }

    public static Endpoints create(final Set<Endpoint> endpoints) {
        return new Endpoints() {
            @Override
            public Endpoint install(Endpoint endpoint) throws MsnosException {
                throw new MsnosException("Cannot install an endpoint, here we are immutable :)", Code.INVALID_STATE);
            }
            
            @Override
            public Endpoint remove(Endpoint endpoint) throws MsnosException {
                throw new MsnosException("Cannot install an endpoint, here we are immutable :)", Code.INVALID_STATE);
            }
            
            @Override
            public Set<? extends Endpoint> all() {
                return endpoints;
            }
        };
    }

}
