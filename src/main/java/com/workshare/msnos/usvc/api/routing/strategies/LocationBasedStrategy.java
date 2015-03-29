package com.workshare.msnos.usvc.api.routing.strategies;

import java.util.ArrayList;
import java.util.List;

import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.geo.Location.Match;
import com.workshare.msnos.usvc.IMicroservice;
import com.workshare.msnos.usvc.api.routing.ApiEndpoint;
import com.workshare.msnos.usvc.api.routing.RoutingStrategy;

public class LocationBasedStrategy implements RoutingStrategy {

    @Override
    public List<ApiEndpoint> select(IMicroservice from, List<ApiEndpoint> apis) {
        final Location target = from.getLocation();
        if (target == null || target == Location.UNKNOWN)
            return apis;
        
        final List<ApiEndpoint> result = new ArrayList<ApiEndpoint>();

        int currentBestMatch = 0;
        for (ApiEndpoint api : apis) {
            final Location location = api.location();
            final Match match = target.match(location);
            final int value = match.value();
            if (value > currentBestMatch) {
                currentBestMatch = value;
                result.clear();
                result.add(api);
            } else if (value == currentBestMatch) {
                result.add(api);
            }
        }
        
        return result;
    }

}
