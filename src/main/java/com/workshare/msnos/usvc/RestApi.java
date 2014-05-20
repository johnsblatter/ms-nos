package com.workshare.msnos.usvc;

import com.workshare.msnos.soup.json.Json;

public class RestApi {

    private String path;
    private int port;
    private String host;
    private transient boolean faulty;

    public RestApi(String path, int port) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        this.faulty = false;
        this.path = path;
        this.port = port;
        this.host = null;
    }

    public RestApi(String path, int port, String host) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        this.faulty = false;
        this.path = path;
        this.port = port;
        this.host = host;
    }

    public RestApi host(String host) {
        return new RestApi(path, port, host);
    }

    public void markAsFaulty() {
        faulty = true;
    }

    public String getPath() {
        return path;
    }

    public boolean isFaulty() {
        return faulty;
    }

    public String getHost() {
        return host;
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
