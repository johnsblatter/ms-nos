package com.workshare.msnos.core.protocols.ip;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.MsnosException.Code;
import com.workshare.msnos.soup.json.Json;

public class BaseEndpoint implements Endpoint {

    private final Type type;
    private final Network network;
    private final short port;

    public BaseEndpoint(Type type, Network host) {
        this(type, host, type.defaultPort());
    }

    public BaseEndpoint(Type type, Network host, short port) {
        if (type == null || host == null)
            throw new IllegalArgumentException("No argument can be null here");

        this.type = type;
        this.network = host;
        this.port = port;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
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
        result = prime * result + network.hashCode();
        result = prime * result + port;
        result = prime * result + type.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            BaseEndpoint other = (BaseEndpoint) obj;
            return network.equals(other.network) && type == other.type && port == other.port;
        } catch (Exception ignore) {
            return false;
        }
    }

    public static final Endpoints create(final BaseEndpoint... points) {
        return create(new HashSet<BaseEndpoint>(Arrays.asList(points)));
    }

    public static final Endpoints create(final Set<BaseEndpoint> endpoints) {
        return new Endpoints() {
            @Override
            public BaseEndpoint install(Endpoint endpoint) throws MsnosException {
                throw new MsnosException("Cannot install an endpoint, here we are immutable :)", Code.INVALID_STATE);
            }
            
            @Override
            public BaseEndpoint remove(Endpoint endpoint) throws MsnosException {
                throw new MsnosException("Cannot install an endpoint, here we are immutable :)", Code.INVALID_STATE);
            }
            
            @Override
            public Set<? extends Endpoint> all() {
                return endpoints;
            }

            @Override
            public Set<? extends Endpoint> of(Agent agent) {
                return endpoints;
            }

            @Override
            public Set<? extends Endpoint> publics() {
                return endpoints;
            }
        };
    }
}
