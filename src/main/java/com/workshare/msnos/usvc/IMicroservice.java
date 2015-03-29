package com.workshare.msnos.usvc;

import java.util.Set;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.usvc.api.RestApi;

public interface IMicroservice {

    public abstract String getName();

    public abstract Set<RestApi> getApis();

    public abstract Location getLocation();

    public abstract Agent getAgent();

}