package com.workshare.msnos.usvc.api;

import com.workshare.msnos.soup.json.Json;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RestApi {

    public enum Type {PUBLIC, INTERNAL, HEALTHCHECK, MSNOS_HTTP}

    private static final AtomicLong NEXT_ID = new AtomicLong(0);

    private final String name;
    private final String path;
    private final String host;
    private final int port;
    private final boolean sessionAffinity;
    private final Type type;
    private final int priority;

    private final transient AtomicInteger tempFaults;
    private final transient long id;

    private transient boolean faulty;


    public RestApi(String name, String path, int port) {
        this(name, path, port, null);
    }

    public RestApi(String name, String path, int port, String host) {
        this(name, path, port, host, Type.PUBLIC, false);
    }

    public RestApi(String name, String path, int port, String host, Type type, boolean sessionAffinity) {
        this(name, path, port, host, type, sessionAffinity, 0);
    }

    public RestApi(String name, String path, int port, String host, Type type, boolean sessionAffinity, int priority) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        this.faulty = false;
        this.name = name;
        this.path = path;
        this.port = port;
        this.host = host;
        this.sessionAffinity = sessionAffinity;
        this.type = type;
        this.priority = priority;
        this.id = NEXT_ID.getAndIncrement();
        tempFaults = new AtomicInteger();
    }

    public RestApi asHealthCheck() {
        return new RestApi(name, path, port, host, Type.HEALTHCHECK, sessionAffinity, priority);
    }

    public RestApi asInternal() {
        return new RestApi(name, path, port, host, Type.INTERNAL, sessionAffinity, priority);
    }

    public RestApi withPriority(int priority) {
        return new RestApi(name, path, port, host, type, sessionAffinity, priority);
    }

    public RestApi onHost(String host) {
        return new RestApi(name, path, port, host, type, sessionAffinity, priority);
    }

    public RestApi onPort(int port) {
        return new RestApi(name, path, port, host, type, sessionAffinity, priority);
    }

    public RestApi withAffinity() {
        return new RestApi(name, path, port, host, type, true, priority);
    }

    public boolean hasAffinity() {
        return sessionAffinity;
    }

    public boolean isFaulty() {
        return faulty;
    }

    public void markFaulty() {
        faulty = true;
    }

    public void markWorking() {
        tempFaults.set(0);
        faulty = false;
    }

    public void markTempFault() {
        tempFaults.incrementAndGet();
    }

    public int getTempFaults() {
        return tempFaults.get();
    }

    public int getPriority() {
        return priority;
    }

    public String getName() {
        return name;
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
        return String.format("http://%s:%d/%s/%s/", getHost(), getPort(), getName(), getPath());
    }

    public long getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RestApi restApi = (RestApi) o;

        if (port != restApi.port) return false;
        if (sessionAffinity != restApi.sessionAffinity) return false;
        if (!name.equals(restApi.name)) return false;
        if (!path.equals(restApi.path)) return false;
        if (type != restApi.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        int prime = 31;
        result = prime * result + path.hashCode();
        result = prime * result + port;
        result = prime * result + (sessionAffinity ? 2 : 1);
        result = prime * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

}
