package com.workshare.msnos.usvc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Cloud.Listener;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.strategies.PriorityRoutingStrategy;

public class Microservice implements IMicroService {

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final String name;
    private final LocalAgent agent;
    private final Location location;
    private final Listener listener;
    private final List<RestApi> localApis;

    private Microcloud cloud;

    public Microservice(String name) {
        this(name, new LocalAgent(UUID.randomUUID()), ExecutorServices.newSingleThreadScheduledExecutor());
    }

    public Microservice(String name, LocalAgent agent) {
        this(name, agent, ExecutorServices.newSingleThreadScheduledExecutor());
    }

    public Microservice(String name, LocalAgent agent, ScheduledExecutorService executor) {
        this.name = name;
        this.agent = agent;
        this.location = Location.computeMostPreciseLocation(agent.getEndpoints());
        this.localApis = new CopyOnWriteArrayList<RestApi>();

        this.listener = new Cloud.Listener() {
            @Override
            public void onMessage(Message message) {
                try {
                    process(message);
                } catch (MsnosException e) {
                    log.error("Error processing message {}", e);
                }
            }
        };
    }

    public Microcloud getCloud() {
        return cloud;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LocalAgent getAgent() {
        return agent;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public RestApi searchApi(String name, String path) {
        return cloud.searchApi(this, name, path);
    }

    public List<RestApi> getLocalApis() {
        return localApis;
    }

    public void publish(RestApi... apis) throws MsnosException {
        final RestApi[] all = enforcePriorityIfRequired(apis);
        localApis.addAll(Arrays.asList(all));
        cloud.publish(this, all);
//        dddd  // TODO Attach the MSONS_HTTP to the cloud - how?
    }
  
    public void join(final Microcloud cumulus) throws MsnosException {
        if (this.cloud != null)
            throw new IllegalArgumentException("The same instance of a microservice cannot join different clouds!");

        cloud = cumulus;
        cloud.onJoin(this);
        cloud.addListener(listener);
    }

    public void leave() throws MsnosException {
        if (cloud == null)
            return;
        
        log.debug("Leaving cloud {}", cloud);
        cloud.removeListener(listener);
        cloud.onLeave(this);
        cloud = null;
        log.debug("So long {}", cloud);
    }

    private void process(Message message) throws MsnosException {
        if (!message.getFrom().equals(agent.getIden())) {
            if (message.getType() == Message.Type.ENQ) {
                processENQ();
            }
        }
    }

    private void processENQ() throws MsnosException {
        Message message = new MessageBuilder(Message.Type.QNE, agent, cloud.getCloud()).with(new QnePayload(name, new HashSet<RestApi>(getLocalApis()))).make();
        agent.send(message);
    }

    @Override
    public String toString() {
        return Json.toJsonString(agent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Microservice that = (Microservice) o;
        return this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + agent.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + cloud.getCloud().hashCode();
        return result;
    }

    private RestApi[] enforcePriorityIfRequired(RestApi[] apis) {
        if (!PriorityRoutingStrategy.isEnabled())
            return apis;

        Integer priority = PriorityRoutingStrategy.getDefaultLevel();
        if (priority != null) {
            for (int i = 0; i < apis.length; i++) {
                apis[i] = apis[i].withPriority(priority);                
            }
        } 

        return apis;
    }

    @Override
    public Set<RestApi> getApis() {
        return new HashSet<RestApi>(localApis);
    }
}
