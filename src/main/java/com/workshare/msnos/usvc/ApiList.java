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

    public void remove(RemoteMicroservice toRemove) {
        for (int i = 0; i < apis.size(); i++)
            if (apis.get(i).remote().equals(toRemove)) {
                apis.remove(i);
                break;
            }
    }

    public List<RestApi> getAll() {
        List<RestApi> result = new ArrayList<RestApi>();
        for (Api api : apis) {
            result.add(api.rest());
        }
        return result;
    }

    public RestApi get() {
        if (apis.size() == 0) return null;
        if (affinite != null && !affinite.isFaulty()) return affinite;

        for (Api checkFaults : apis)
            if (checkFaults.rest().isFaulty()) {
                faultyService = checkFaults.remote();
            }

        RestApi result = getWithRoundRobinNotFaulty();

        if (result != null && result.hasAffinity()) affinite = result;

        return result;
    }

    private RestApi getWithRoundRobinNotFaulty() {
        Api api = getWithRoundRobin();

        if (api.rest().isFaulty() && apis.size() == 1) return null;
        else if (api.rest().isFaulty()) api = getWithRoundRobin();
        else if (api.remote().equals(faultyService)) {
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
