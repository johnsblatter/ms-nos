package com.workshare.msnos.usvc.api.routing;

import com.workshare.msnos.usvc.Microservice;
import com.workshare.msnos.usvc.RemoteMicroservice;
import com.workshare.msnos.usvc.api.RestApi;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApiRepository {

    private final Map<String, ApiList> remoteApis;

    public ApiRepository() {
        this.remoteApis = new ConcurrentHashMap<String, ApiList>();
    }

    public Map<String, ApiList> getRemoteApis() {
        return remoteApis;
    }

    // TODO put a concurrent map here for performance reasons
    public RestApi searchApiById(long id) throws Exception {
        Collection<ApiList> apiListCol = getRemoteApis().values();
        for (ApiList apiList : apiListCol) {
            for (RestApi rest : apiList.getApis()) {
                if (rest.getId() == id) {
                    return rest;
                }
            }
        }
        return null;
    }

    public RestApi searchApi(Microservice from, String name, String path) {
        String key = name + path;
        ApiList apiList = getRemoteApis().get(key);
        return apiList == null ? null : apiList.get(from);
    }

    public void register(RemoteMicroservice remote) {
        for (RestApi rest : remote.getApis()) {
            String key = rest.getName() + rest.getPath();
            if (getRemoteApis().containsKey(key)) {
                getRemoteApis().get(key).add(remote, rest);
            } else {
                ApiList apiList = new ApiList();
                apiList.add(remote, rest);
                getRemoteApis().put(key, apiList);
            }
        }
    }

    public void unregister(RemoteMicroservice faulty) {
        for (RestApi rest : faulty.getApis()) {
            String key = rest.getName() + rest.getPath();
            if (getRemoteApis().containsKey(key)) {
                ApiList apiList = getRemoteApis().get(key);
                apiList.remove(faulty);
                break;
            }
        }
    }

}
