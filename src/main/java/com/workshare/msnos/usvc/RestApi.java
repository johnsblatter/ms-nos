package com.workshare.msnos.usvc;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.soup.json.Json;

public class RestApi {

    private String path;
    private int port;
    private String host;
    private transient boolean faulty;
    private Agent agent;

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
        this.agent = null;
    }

    public RestApi(String path, int port, String host, Agent agent) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        this.faulty = false;
        this.path = path;
        this.port = port;
        this.host = host;
        this.agent = agent;
    }

    public RestApi host(String host) {
        return new RestApi(path, port, host);
    }

    public RestApi agent(Agent agent) {
        return new RestApi(path, port, host, agent);
    }

    public void markAsFaulty() {
        faulty = true;
    }

    public String getPath() {
        return path;
    }

    public int getPort() {
        return port;
    }

    public boolean isFaulty() {
        return faulty;
    }

    public String getHost() {
        return host;
    }

    public Agent getAgent() {
        return agent;
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
            return path.equals(other.path) && port == other.port && faulty == other.faulty && agent.equals(other.agent);
        } catch (Exception any) {
            return false;
        }
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }
}
