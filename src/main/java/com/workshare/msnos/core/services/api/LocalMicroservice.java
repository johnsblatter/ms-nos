package com.workshare.msnos.core.services.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Gateways;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.cloud.LocalAgent;
import com.workshare.msnos.core.geo.Location;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.AddressResolver;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.services.api.routing.strategies.PriorityRoutingStrategy;
import com.workshare.msnos.soup.json.Json;

public class LocalMicroservice implements Microservice {

    private static final AddressResolver ADDRESS_RESOLVER = new AddressResolver();

    private static final Logger log = LoggerFactory.getLogger("STANDARD");

    private final String name;
    private final LocalAgent agent;
    private final Location location;
    private final List<RestApi> localApis;

    transient private MicroservicesHub cloud;

    public LocalMicroservice(String name) {
        this(name, new LocalAgent(UUID.randomUUID()));
    }

    public LocalMicroservice(String name, LocalAgent agent) {
        this.name = name;
        this.agent = agent;
        this.location = computeLocation(agent);
        this.localApis = new CopyOnWriteArrayList<RestApi>();
    }

    private Location computeLocation(LocalAgent agent) {
        final Set<Endpoint> points = endpoints(agent);

        try {
            Network externalIP = ADDRESS_RESOLVER.findRouterIP();
            if (externalIP != null) {
                points.add(new BaseEndpoint(Type.HTTP, externalIP));
            }
        } catch (Throwable ex) {
            log.warn("Unable to compute external IP", ex);
        }

        return Location.computeMostPreciseLocation(points);
    }

    public Set<Endpoint> endpoints(LocalAgent agent) {
        Set<Endpoint> points = new HashSet<Endpoint>();
        try {
            points.addAll(Gateways.allEndpoints());
        } catch (MsnosException e) {
            log.warn("Unable to endpoints from gateways... wooot?", e);
        }
        points.addAll(agent.getEndpoints());
        return points;
    }

    public MicroservicesHub getCloud() {
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

    public RestApi searchApi(String path) {
        return cloud.searchApi(this, path);
    }

    public List<RestApi> getLocalApis() {
        return localApis;
    }

    public void publish(RestApi... apis) throws MsnosException {
        final RestApi[] all = enforcePriorityIfRequired(apis);
        localApis.addAll(Arrays.asList(all));
        cloud.publish(this, all);
    }

    public void join(final MicroservicesHub cumulus) throws MsnosException {
        if (this.cloud != null)
            throw new IllegalArgumentException("The same instance of a microservice cannot join different clouds!");

        cloud = cumulus;
        cloud.onJoin(this);
        
        cloud.getCloud().getRing().onMicroserviceJoin(this);
    }

    public void leave() throws MsnosException {
        if (cloud == null)
            return;

        log.debug("Leaving cloud {}", cloud);
        cloud.onLeave(this);
        cloud = null;
        log.debug("So long {}", cloud);
    }

    protected void process(Message message) throws MsnosException {
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
        LocalMicroservice that = (LocalMicroservice) o;
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
