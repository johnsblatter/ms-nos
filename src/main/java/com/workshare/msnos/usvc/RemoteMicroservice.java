package com.workshare.msnos.usvc;

import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RemoteMicroservice implements IMicroservice {

    private final RemoteAgent agent;
    private final String name;
    private final Set<RestApi> apis;
    private final AtomicBoolean faulty;
    private final AtomicLong lastUpdated;
    private final AtomicLong lastChecked;

    private Location location;
    
    public RemoteMicroservice(String name, RemoteAgent agent, Set<RestApi> apis) {
        this.name = name;
        this.agent = agent;
        this.apis = RestApi.ensureHostIsPresent(agent, apis);
        this.faulty = new AtomicBoolean(false);
        this.lastUpdated = new AtomicLong(SystemTime.asMillis());
        this.lastChecked = new AtomicLong(SystemTime.asMillis());
        this.location = Location.computeMostPreciseLocation(agent.getEndpoints());
        
        final Ring ring = agent.getRing();
        if (ring != null) {
            ring.onMicroserviceJoin(this);
            this.location = ring.location();
        } 
   }

    protected void setApis(Set<RestApi> restApis) {
        lastUpdated.set(SystemTime.asMillis());
        synchronized (apis) {
            apis.addAll(RestApi.ensureHostIsPresent(agent, restApis));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<RestApi> getApis() {
        synchronized (apis) {
            return Collections.unmodifiableSet(apis);
        }
    }
    @Override
    public RemoteAgent getAgent() {
        return agent;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("name: '%s', apis: %s, location: %s", name, apis, location);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteMicroservice that = (RemoteMicroservice) o;
        return this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + agent.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public void markWorking() {
        lastChecked.set(SystemTime.asMillis());
        faulty.set(false);
        agent.touch();
        for (RestApi rest : getApis()) {
            rest.markWorking();
        }
    }

    public void markFaulty() {
        lastChecked.set(SystemTime.asMillis());
        faulty.set(true);
        for (RestApi rest : getApis()) {
            rest.markFaulty();
        }
    }
    
    public boolean isFaulty() {
        return faulty.get();
    }

    public long getLastUpdated() {
        return lastUpdated.get();
    }

    public long getLastChecked() {
        return lastChecked.get();
    }

    public UUID getUuid() {
        return agent.getIden().getUUID();
    }
}