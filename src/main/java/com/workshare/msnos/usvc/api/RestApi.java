package com.workshare.msnos.usvc.api;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.json.Json;

public class RestApi {

    private static final Logger log = LoggerFactory.getLogger(RestApi.class);

    public enum Type {
        @SerializedName("PUB")
        PUBLIC, 
        @SerializedName("INT")
        INTERNAL, 
        @SerializedName("HCK")
        HEALTHCHECK, 
        @SerializedName("MHT")
        MSNOS_HTTP
    }

    private static final AtomicLong NEXT_ID = new AtomicLong(0);

    private final transient long id;

    private final String path;
    private final String host;
    private final int port;
    private final boolean sticky;
    private final Type type;
    private final int priority;
    
    private final AtomicInteger tempFaults;
    private boolean faulty;

    public RestApi(String path, int port) {
        this(path, port, null);
    }

    public RestApi(String path, int port, String host) {
        this(path, port, host, Type.PUBLIC, false);
    }

    public RestApi(String path, int port, String host, Type type, boolean sessionAffinity) {
        this(path, port, host, type, sessionAffinity, 0);
    }

    public RestApi(String path, int port, String host, Type type, boolean sessionAffinity, int priority) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        this.path = path;
        this.faulty = false;       
        this.port = port;
        this.host = host;
        this.sticky = sessionAffinity;
        this.type = type;
        this.priority = priority;
        this.id = NEXT_ID.getAndIncrement();
        tempFaults = new AtomicInteger();
    }

    public RestApi asHealthCheck() {
        return new RestApi(path, port, host, Type.HEALTHCHECK, sticky, priority);
    }

    public RestApi asInternal() {
        return new RestApi(path, port, host, Type.INTERNAL, sticky, priority);
    }

    public RestApi asMsnosEndpoint() {
        return new RestApi(path, port, host, Type.MSNOS_HTTP, sticky, priority);
    }

    public RestApi withPriority(int priority) {
        return new RestApi(path, port, host, type, sticky, priority);
    }

    public RestApi onHost(String host) {
        return new RestApi(path, port, host, type, sticky, priority);
    }

    public RestApi onPort(int port) {
        return new RestApi(path, port, host, type, sticky, priority);
    }

    public RestApi withAffinity() {
        return new RestApi(path, port, host, type, true, priority);
    }

    public boolean hasAffinity() {
        return sticky;
    }

    public boolean isFaulty() {
        return faulty;
    }

    public RestApi markFaulty() {
        faulty = true;
        return this;
    }

    public void markWorking() {
        tempFaults.set(0);
        faulty = false;
    }

    public RestApi markTempFault() {
        tempFaults.incrementAndGet();
        return this;
    }

    public int getTempFaults() {
        return tempFaults.get();
    }

    public int getPriority() {
        return priority;
    }

    public String getPath() {
        return path;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        String hostname = getHost();
        if (hostname == null)
            hostname = "*";
        
        String prefix = "";
        String path = getPath();
        if (!path.startsWith("/"))
            prefix = "/";
        
        return String.format("http://%s:%d%s%s", hostname, getPort(), prefix, getPath());
    }

    public long getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        int prime = 31;
        result = prime * result + port;
        result = prime * result + (sticky ? 1231 : 1237);
        result = prime * result + type.hashCode();
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        return result;
    }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RestApi restApi = (RestApi) o;

        if (port != restApi.port) return false;
        if (sticky != restApi.sticky) return false;
        if (!path.equals(restApi.path)) return false;
        if (type != restApi.type) return false;
        if (!host.equals(restApi.host)) return false;

        return true;
    }

    @Override
    public String toString() {
        try {
            JsonObject obj = (JsonObject)Json.toJsonTree(this);
            obj.addProperty("faulty",this.isFaulty());
            obj.addProperty("tempfaults",this.getTempFaults());
            return obj.toString();
        } catch (Exception any) {
            try {
                return getUrl();
            } catch (Exception e) {
                return super.toString();
            }
        }
    }

    public static Set<RestApi> ensureHostIsPresent(Agent agent, Set<RestApi> apis) {
        Set<RestApi> result = new HashSet<RestApi>();
        for (RestApi api : apis) {
            if (api.getHost() == null || api.getHost().isEmpty()) {
                boolean found = false;
                for (Endpoint endpoint : agent.getEndpoints()) {
                    Network network = endpoint.getNetwork();
                    result.add(api.onHost(network.getHostString()));
                    found = true;
                }
                if (!found) {
                    log.error("{} API received but agent with no endpoints!!! {}", api, agent);
                }
            } else {
                result.add(api);
            }
        }
        return result;
    }

}
