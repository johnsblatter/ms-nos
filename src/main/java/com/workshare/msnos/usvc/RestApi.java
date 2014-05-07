package com.workshare.msnos.usvc;

import com.workshare.msnos.soup.json.Json;

public class RestApi {

    private String path;
    private int port;
    private transient boolean faulty;

    public RestApi(String path, int port) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        this.faulty = false;
        this.path = path;
        this.port = port;
    }

    public String getPath() {
        return path;
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
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            RestApi other = (RestApi) obj;
            return path.equals(other.path) && port == other.port && faulty == other.faulty;
        } catch (Exception any) {
            return false;
        }
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    public void markAsFaulty() {
        faulty = true;
    }

    public boolean isFaulty() {
        return faulty;
    }
}
