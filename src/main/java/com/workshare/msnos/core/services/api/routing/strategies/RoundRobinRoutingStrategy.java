package com.workshare.msnos.core.services.api.routing.strategies;

import java.util.List;

import com.workshare.msnos.core.services.api.Microservice;
import com.workshare.msnos.core.services.api.RemoteMicroservice;
import com.workshare.msnos.core.services.api.routing.ApiEndpoint;
import com.workshare.msnos.core.services.api.routing.RoutingStrategy;
import com.workshare.msnos.soup.SingleElementList;

public class RoundRobinRoutingStrategy implements RoutingStrategy {

    private int index = 0;
   
    @Override
    public List<ApiEndpoint> select(Microservice from, List<ApiEndpoint> apis) {
        List<ApiEndpoint> result = new SingleElementList<ApiEndpoint>();

        RemoteMicroservice skipMe = null;
        for (int attempt = 0; attempt < apis.size(); attempt++) {
            final int current = (index + attempt) % apis.size();
            ApiEndpoint api = apis.get(current);
            if (api.isFaulty()) {
                skipMe = api.service();
                continue;
            }

            if (api.belongsTo(skipMe))
                continue;

            index = current + 1;
            result.add(api);
            break;
        }

        return result;
    }

}
