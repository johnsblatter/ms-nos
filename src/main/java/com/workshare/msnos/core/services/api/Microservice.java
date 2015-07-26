package com.workshare.msnos.core.services.api;

import java.util.Set;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.geo.Location;

public interface Microservice {

    public abstract String getName();

    public abstract Set<RestApi> getApis();

    public abstract Location getLocation();

    public abstract Agent getAgent();

}