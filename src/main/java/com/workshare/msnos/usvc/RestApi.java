package com.workshare.msnos.usvc;

import com.workshare.msnos.soup.json.Json;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RestApi {

    public enum Type {HEALTHCHECK, PUBLIC, INTERNAL}

    private static final AtomicLong NEXT_ID = new AtomicLong(0);

    private final int port;
    private final String name;
    private final String path;
    private final String host;
    private final boolean sessionAffinity;
    private final Type type;

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
        this.id = NEXT_ID.getAndIncrement();
        tempFaults = new AtomicInteger();
    }

    public RestApi asHealthCheck() {
        return new RestApi(name, path, port, host, Type.HEALTHCHECK, sessionAffinity);
    }

    public RestApi asInternal() {
        return new RestApi(name, path, port, host, Type.INTERNAL, sessionAffinity);
    }

    public RestApi onHost(String host) {
        return new RestApi(name, path, port, host, type, sessionAffinity);
    }

    public RestApi withAffinity() {
        return new RestApi(name, path, port, host, type, true);
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
        return String.format("http://%s:%d/%s/%s/", getHost().substring(0, getHost().indexOf("/")), getPort(), getName(), getPath());
    }

    public long getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + path.hashCode();
        result = prime * result + port;
        if (host != null) result = prime * result + host.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            RestApi other = (RestApi) obj;
            return this.hashCode() == other.hashCode() && faulty == other.faulty;
        } catch (Exception any) {
            return false;
        }
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }
}
