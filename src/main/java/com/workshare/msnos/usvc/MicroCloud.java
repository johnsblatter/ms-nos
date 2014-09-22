package com.workshare.msnos.usvc;

import com.workshare.msnos.core.*;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.ApiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.workshare.msnos.core.Cloud.Listener;

class MicroCloud {

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final List<RestApi> localApis;
    private final Map<Iden, RemoteMicroservice> microServices;
    private final Map<UUID, PassiveService> passiveServices;
    private final ApiRepository apis;

    private Cloud cloud;

    MicroCloud(Cloud cloud) {
        this.cloud = cloud;

        localApis = new CopyOnWriteArrayList<RestApi>();
        microServices = new ConcurrentHashMap<Iden, RemoteMicroservice>();
        passiveServices = new ConcurrentHashMap<UUID, PassiveService>();
        apis = new ApiRepository();
    }

    Listener addListener(Listener listener) {
        return cloud.addListener(listener);
    }

    Cloud getCloud() {
        return cloud;
    }

    ApiRepository getApis() {
        return apis;
    }

    List<RestApi> getLocalApis() {
        return localApis;
    }

    Map<Iden, RemoteMicroservice> getMicroservices() {
        return microServices;
    }

    Map<UUID, PassiveService> getPassiveServices() {
        return passiveServices;
    }

    void onJoin(Microservice microservice) throws MsnosException {
        LocalAgent agent = microservice.getAgent();
        agent.join(cloud);

        Message message = new MessageBuilder(Message.Type.ENQ, agent, cloud).make();
        agent.send(message);
    }

    void publish(Microservice microservice, RestApi... api) throws MsnosException {
        List<RestApi> restApis = evaluateApiPriority(api);

        publishAndAddToLocalApis(microservice, restApis, microservice.getName(), getApisAsArray(restApis));
    }

    void passiveJoin(PassiveService passiveService) {
        passiveServices.put(passiveService.getUuid(), passiveService);
    }

    void passivePublish(PassiveService passiveService, Microservice microservice, RestApi... apis) throws MsnosException {
        if (passiveServices.containsKey(passiveService.getUuid())) {
            List<RestApi> restApis = evaluateApiPriority(apis);

            publishAndAddToLocalApis(microservice, restApis, passiveService.getName(), getApisAsArray(restApis));
        } else {
            throw new IllegalArgumentException("Cannot publish passive restApis that are from services which are not joined to the Cloud! ");
        }
    }

    private List<RestApi> evaluateApiPriority(RestApi[] api) {
        boolean mode = Boolean.getBoolean("high.priority.mode");
        List<RestApi> restApis = Arrays.asList(api);

        if (mode) {
            Integer priority = Integer.getInteger("priority.level");
            if (priority != null) {
                for (RestApi restApi : api) {
                    Collections.replaceAll(restApis, restApi, restApi.withPriority(priority));
                }
            } else {
                log.error("Priority level not set, unable to publish RestApis with priority. Publishing apis with no priority level.");
            }
        }
        return restApis;
    }

    private void publishAndAddToLocalApis(Microservice microservice, List<RestApi> restApis, String name, RestApi[] apis) throws MsnosException {
        LocalAgent agent = microservice.getAgent();
        Message message = new MessageBuilder(Message.Type.QNE, agent, cloud).with(new QnePayload(name, apis)).make();

        agent.send(message);
        localApis.addAll(restApis);
    }

    private RestApi[] getApisAsArray(List<RestApi> restApis) {
        return restApis.toArray(new RestApi[restApis.size()]);
    }
}