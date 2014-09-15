package com.workshare.msnos.usvc.api.routing;

import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.usvc.RemoteMicroservice;
import com.workshare.msnos.usvc.api.RestApi;

public class ApiEndpoint {

    private final RemoteMicroservice remote;
    private final RestApi api;
    private final Location location;

    public ApiEndpoint(RemoteMicroservice remote, RestApi api, Location location) {
        this.remote = remote;
        this.api = api;
        this.location = location;
    }

    public boolean belongsTo(RemoteMicroservice remote) {
        return remote == null ? false : remote.equals(this.remote);
    }

    public RestApi api() {
        return api;
    }

    public RemoteMicroservice service() {
        return remote;
    }

    public Location location() {
        return location;
    }

    public boolean isFaulty() {
        return api.isFaulty();
    }

    public int priority() {
        return api.getPriority();
    }

    @Override
    public String toString() {
        return remote.getName() + "::" + api.getName() + "@" + location;
    }
}