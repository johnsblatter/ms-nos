package com.workshare.msnos.usvc.api.routing;

import com.workshare.msnos.soup.threading.ConcurrentBuildingMap;
import com.workshare.msnos.usvc.Microservice;
import com.workshare.msnos.usvc.RemoteMicroservice;
import com.workshare.msnos.usvc.api.RestApi;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ApiRepository {

    private final Map<String, ApiList> remoteApis;

    public ApiRepository() {
        this.remoteApis = new ConcurrentBuildingMap<String, ApiList>(new ConcurrentBuildingMap.Factory<ApiList>() {
            @Override
            public ApiList make() {
                return new ApiList();
            }
        });
    }

    public Map<String, ApiList> getRemoteApis() {
        return remoteApis;
    }

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

    public RestApi searchApi(Microservice from, String path) {
        ApiList apiList = getRemoteApis().get(path);
        return apiList == null ? null : apiList.get(from);
    }

    public void register(RemoteMicroservice remote) {
        Set<RestApi> apis = new CopyOnWriteArraySet<RestApi>(remote.getApis());

        for (RestApi rest : apis) {
            final String key = rest.getPath();
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
            final String key = rest.getPath();
            if (getRemoteApis().containsKey(key)) {
                ApiList apiList = getRemoteApis().get(key);
                apiList.remove(faulty);
            }
        }
    }

}
