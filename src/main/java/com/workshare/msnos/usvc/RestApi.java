package com.workshare.msnos.usvc;

import com.workshare.msnos.soup.json.Json;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RestApi {

    private static final AtomicLong NEXT_ID = new AtomicLong(0);

    private final int port;
    private final String name;
    private final String path;
    private final String host;
    private final boolean sessionAffinity;
    private final boolean healthCheck;

    private final transient AtomicInteger tempFaults;

    private transient final long id;
    private transient boolean faulty;

    public RestApi(String name, String path, int port) {
        this(name, path, port, null);
    }

    public RestApi(String name, String path, int port, String host) {
        this(name, path, port, host, false);
    }

    public RestApi(String name, String path, int port, String host, boolean sessionAffinity) {
        this(name, path, port, host, sessionAffinity, false);
    }

    public RestApi(String name, String path, int port, String host, boolean sessionAffinity, boolean healthCheck) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        this.faulty = false;
        this.name = name;
        this.path = path;
        this.port = port;
        this.host = host;
        this.sessionAffinity = sessionAffinity;
        this.healthCheck = healthCheck;
        this.id = NEXT_ID.getAndIncrement();
        tempFaults = new AtomicInteger();
    }

    public RestApi asHealthCheck() {
        return new RestApi(name, path, port, host, sessionAffinity, true);
    }

    public RestApi onHost(String host) {
        return new RestApi(name, path, port, host, sessionAffinity, healthCheck);
    }

    public RestApi withAffinity() {
        return new RestApi(name, path, port, host, true, healthCheck);
    }

    public boolean hasAffinity() {
        return sessionAffinity;
    }

    public boolean isHealthCheck() {
        return healthCheck;
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
