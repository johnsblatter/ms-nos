package com.workshare.msnos.core.services.api.routing;

import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.services.api.RemoteMicroservice;
import com.workshare.msnos.core.services.api.RestApi;

public class ApiEndpoint {

    private final RemoteMicroservice remote;
    private final RestApi api;

    public ApiEndpoint(RemoteMicroservice remote, RestApi api) {
        if (remote == null)
            throw new IllegalArgumentException("Cannot create an API endpoint with null remote!");
        if (api == null)
            throw new IllegalArgumentException("Cannot create an API endpoint with null api!");
        
        this.remote = remote;
        this.api = api;
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
        return remote.getLocation();
    }

    public boolean isFaulty() {
        return api.isFaulty();
    }

    public int priority() {
        return api.getPriority();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((api == null) ? 0 : api.hashCode());
        result = prime * result + ((remote == null) ? 0 : remote.getUuid().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        ApiEndpoint other = (ApiEndpoint) obj;
        return  this.api.getUrl().equals(other.api.getUrl()) &&
                this.remote.getUuid().equals(other.remote.getUuid());
    }

    @Override
    public String toString() {
        return remote.getName() + "::" + api.getUrl() + "@" + location();
    }
}