package com.workshare.msnos.usvc;

import com.workshare.msnos.soup.json.Json;

public class RestApi {

    private String name;
    private String path;
    private int port;
    private String host;
    private transient boolean faulty;
    private boolean sessionAffinity;

    public RestApi(String name, String path, int port) {
        this(name, path, port, null);
    }

    public RestApi(String name, String path, int port, String host) {
        this(name, path, port, host, false);
    }

    public RestApi(String name, String path, int port, String host, boolean sessionAffinity) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        this.faulty = false;
        this.sessionAffinity = sessionAffinity;
        this.name = name;
        this.path = path;
        this.port = port;
        this.host = host;
    }

    public RestApi host(String host) {
        return new RestApi(name, path, port, host);
    }

    public RestApi withAffinity() {
        return new RestApi(name, path, port, host, true);
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
        faulty = false;
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
