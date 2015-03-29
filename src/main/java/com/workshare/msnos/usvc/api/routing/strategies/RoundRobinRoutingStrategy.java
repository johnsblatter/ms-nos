package com.workshare.msnos.usvc.api.routing.strategies;

import java.util.List;

import com.workshare.msnos.soup.SingleElementList;
import com.workshare.msnos.usvc.IMicroservice;
import com.workshare.msnos.usvc.RemoteMicroservice;
import com.workshare.msnos.usvc.api.routing.ApiEndpoint;
import com.workshare.msnos.usvc.api.routing.RoutingStrategy;

public class RoundRobinRoutingStrategy implements RoutingStrategy {

    private int index = 0;
   
    @Override
    public List<ApiEndpoint> select(IMicroservice from, List<ApiEndpoint> apis) {
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
