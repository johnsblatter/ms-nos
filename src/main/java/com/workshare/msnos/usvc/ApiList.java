package com.workshare.msnos.usvc;

import java.util.ArrayList;
import java.util.List;

public class ApiList {

    private final List<Api> apis;

    private RemoteMicroservice faultyService;
    private RestApi affinite;
    private int index;

    public ApiList() {
        apis = new ArrayList<Api>();
        index = 0;
    }

    public void add(RemoteMicroservice remote, RestApi rest) {
        apis.add(new Api(remote, rest));
    }

    public RestApi get() {
        if (affinite != null && !affinite.isFaulty()) return affinite;

//        TODO Write a test with multiple faulty services, make sure that there is an effective way of keeping a reference to each
//        TODO and adhering to correct selection algorithm.
        for (Api fault : apis) if (fault.rest().isFaulty()) faultyService = fault.remote();

        RestApi result = getWithRoundRobinNotFaulty();
        if (result != null && result.hasAffinity()) affinite = result;

        return result;
    }

    private RestApi getWithRoundRobinNotFaulty() {
        Api api = getWithRoundRobin();
        if (api.rest().isFaulty() && apis.size() == 1) return null;
        if (api.rest().isFaulty() || api.remote().equals(faultyService)) {
            api = getWithRoundRobin();
            faultyService = null;
        }
        return api.rest();
    }

    private Api getWithRoundRobin() {
        return apis.get(index++ % apis.size());
    }

    private class Api {
        private final RemoteMicroservice remote;
        private final RestApi api;

        public Api(RemoteMicroservice remote, RestApi api) {
            this.remote = remote;
            this.api = api;
        }

        public RestApi rest() {
            return api;
        }

        public RemoteMicroservice remote() {
            return remote;
        }
    }
}
