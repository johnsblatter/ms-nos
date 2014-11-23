package com.workshare.msnos.usvc;

import static com.workshare.msnos.core.CoreHelper.asPublicNetwork;
import static com.workshare.msnos.core.CoreHelper.asSet;
import static com.workshare.msnos.core.CoreHelper.fakeSystemTime;
import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static com.workshare.msnos.core.CoreHelper.randomUUID;
import static com.workshare.msnos.core.MessagesHelper.newAPPMessage;
import static com.workshare.msnos.core.MessagesHelper.newFaultMessage;
import static com.workshare.msnos.core.MessagesHelper.newHCKMessage;
import static com.workshare.msnos.core.MessagesHelper.newLeaveMessage;
import static com.workshare.msnos.core.MessagesHelper.newQNEMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Cloud.Listener;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.RemoteEntity;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.strategies.CachingRoutingStrategy;

public class MicrocloudTest {

    private Microcloud microcloud;
    private Cloud cloud;
    private Microservice local;


    @BeforeClass
    public static void stopClock() {
        System.setProperty(CachingRoutingStrategy.SYSP_TIMEOUT, "0");
    }

    @AfterClass
    public static void restartClock() {
        SystemTime.reset();
    }

    @Before
    public void prepare() throws MsnosException {
        cloud = Mockito.mock(Cloud.class);
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, new UUID(111, 111)));
        microcloud = new Microcloud(cloud, Mockito.mock(ScheduledExecutorService.class));

        LocalAgent agent = mock(LocalAgent.class);
        when(agent.getIden()).thenReturn(newAgentIden());        
        local = new Microservice("fluffy", agent);
        local.join(microcloud);
    }
    
    @Test
    public void shouldCreateRemoteMicroserviceOnQNE() throws IOException {
        RemoteAgent remoteAgent = newRemoteAgent();

        simulateMessageFromCloud(newQNEMessage(remoteAgent, "content"));

        assertAgentInMicroserviceList(remoteAgent);
    }

    @Test
    public void shouldUpdateLastcheckedRemoteMicroserviceOnHCK() throws IOException {
        RemoteMicroservice remote = setupRemoteMicroservice("10.10.10.10", "remote", "/endpoint");

        fakeSystemTime(123456789);
        simulateMessageFromCloud(newHCKMessage(remote, true));

        remote = microcloud.getMicroServices().get(0);
        assertEquals(123456789, remote.getLastChecked());
    }

    @Test
    public void shouldCreateBoundRestApisWhenRestApiNotBound() throws Exception {
        RemoteEntity remoteAgent = newRemoteAgentWithFakeHosts("10.10.10.10");

        RestApi unboundApi = new RestApi("test", "/test", 9999);
        simulateMessageFromCloud(newQNEMessage(remoteAgent, "content", unboundApi));
 
        RemoteMicroservice remote = microcloud.getMicroServices().get(0);
        RestApi api = getFirstRestApi(remote);
        assertEquals(api.getHost(), "10.10.10.10");
    }

    @Test
    public void shouldUpdateMicroserviceApisIfPresent() throws Exception {
        UUID uuid = new UUID(11, 22);
        simulateRemoteMicroserviceJoin(uuid, "24.24.24.24", "remote", createRestApi("content", "/files"));
        simulateRemoteMicroserviceJoin(uuid, "24.24.24.24", "remote", createRestApi("content", "/healthcheck"));

        assertEquals(1, microcloud.getMicroServices().size());
        assertEquals(2, microcloud.getMicroServices().get(0).getApis().size());
    }

    @Test
    public void shouldReturnCorrectApiWhenSearchingById() throws Exception {
        RestApi api1 = getFirstRestApi(setupRemoteMicroservice("24.24.24.24", "content", "/files"));
        RestApi api2 = getFirstRestApi(setupRemoteMicroservice("11.11.11.11", "content", "/folders"));
        RestApi api3 = getFirstRestApi(setupRemoteMicroservice("23.23.23.23", "content", "/files"));
        RestApi api4 = getFirstRestApi(setupRemoteMicroservice("25.22.22.22", "content", "/files"));

        assertEquals(api1, microcloud.searchApiById(api1.getId()));
        assertEquals(api2, microcloud.searchApiById(api2.getId()));
        assertEquals(api3, microcloud.searchApiById(api3.getId()));
        assertEquals(api4, microcloud.searchApiById(api4.getId()));
    }

    @Test
    public void shouldRemoveRemoteApisOnAgentFault() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroservice("24.24.24.24", "content", "/files");
        RestApi api = getFirstRestApi(remote);
        assertEquals(api, microcloud.searchApi(local, "content", "/files"));

        simulateMessageFromCloud(newFaultMessage(remote.getAgent()));
        assertNull(microcloud.searchApi(local, "content", "/files"));
    }

    @Test
    public void shouldRemoveMicroserviceOnAgentFault() throws Exception {
        RemoteMicroservice remoteMicroservice = setupRemoteMicroservice("10.10.10.10", "remote", "/endpoint");

        simulateMessageFromCloud(newFaultMessage(remoteMicroservice.getAgent()));

        assertFalse(microcloud.getMicroServices().contains(remoteMicroservice));
    }

    @Test
    public void shouldRemoveMicroserviceOnAgentLeave() throws Exception {
        RemoteMicroservice remoteMicroservice = setupRemoteMicroservice("10.10.10.10", "remote", "/endpoint");

        simulateMessageFromCloud(newLeaveMessage(remoteMicroservice.getAgent()));

        assertFalse(microcloud.getMicroServices().contains(remoteMicroservice));
    }

    @Test
    public void shouldBeAbleToMarkRestApiAsFaulty() throws Exception {
        String endpoint = "/files";

        setupRemoteMicroservice("10.10.10.10", "content", endpoint);
        setupRemoteMicroservice("10.10.10.11", "content", endpoint);

        RestApi result1 = microcloud.searchApi(local, "content", endpoint);
        result1.markFaulty();

        RestApi result2 = microcloud.searchApi(local, "content", endpoint);
        assertNotEquals(result2.getId(), result1.getId());
    }

    @Test
    public void shouldReturnCorrectRestApi() throws Exception {
        setupRemoteMicroservice("10.10.10.10", "content", "/files");
        setupRemoteMicroservice("10.10.10.10", "content", "/folders");

        RestApi result = microcloud.searchApi(local, "content", "/files");

        assertTrue("/files".equals(result.getPath()));
    }

    @Test
    public void shouldNOTSelectApisExposedBySelf() throws Exception {
        local.publish(new RestApi("test", "alfa", 1234));
        
        RemoteMicroservice remote = setupRemoteMicroservice("10.10.10.10", "test", "alfa");
        RestApi expected = getFirstRestApi(remote);
        
        assertEquals(expected, local.searchApi("test", "alfa"));
        assertEquals(expected, microcloud.searchApi(local, "test", "alfa"));
    }


    @Test
    public void shouldSearchesReturnNullIfApiListIsEmpty() throws Exception {
        assertNull(microcloud.searchApi(local, "something", "don't care"));
        assertNull(microcloud.searchApiById(1033l));
    }

    @Test
    public void shouldAddPassiveToListOnPassiveJoin() throws Exception {
        PassiveService passiveService = new PassiveService(microcloud, "testPassive", "10.10.10.10", 9999, "http://10.10.10.10/healthcheck/");

        passiveService.join();

        assertEquals(1, microcloud.getPassiveServices().size());
    }

    @Test
    public void shouldBeAbleToSearchForPassiveServicesByUUID() throws Exception {
        PassiveService passiveService = new PassiveService(microcloud, "testPassive", "10.10.10.10", 9999, "http://10.10.10.10/healthcheck/");
        passiveService.join();

        UUID search = passiveService.getUuid();

        assertEquals(microcloud.searchPassives(search), passiveService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldOnlyAllowJoinedPassivelyToPublishApis() throws Exception {
        PassiveService unjoined = new PassiveService(microcloud, "testUnjoined", "10.10.10.10", 9999, "http://10.11.11.11/healthcheck/");
        unjoined.publish(mock(RestApi.class));
    }

    @Test
    public void shouldRegisterRemoteMsnosEndpoints() throws Exception {
        final String host = "24.24.24.24";
        final RestApi api = createMsnosApi(host);
        RemoteMicroservice remote = simulateRemoteMicroserviceJoin(randomUUID(), host, "remote", api);

        HttpEndpoint endpoint = new HttpEndpoint(remote, api);
        verify(cloud).registerRemoteMsnosEndpoint(endpoint);
    }

    @Test
    public void shouldRegisterLocalMsnosEndpointsOnPublish() throws Exception {
        final String host = "24.24.24.24";
        final RestApi api = createMsnosApi(host);
        ensureLocalNetworkPresent(asPublicNetwork(api.getHost()));

        local.publish(api);
 
        HttpEndpoint endpoint = new HttpEndpoint(local, api);
        verify(cloud).registerLocalMsnosEndpoint(endpoint);
    }

    @Test
    public void shouldRegisterLocalAnonymousMsnosEndpointsOnPublish() throws Exception {
        final String host = "21.21.21.21";
        final RestApi api = createMsnosApi(null);
        ensureLocalNetworkPresent(asPublicNetwork(host));

        local.publish(api);
 
        HttpEndpoint endpoint = new HttpEndpoint(local, api.onHost(host));
        verify(cloud).registerLocalMsnosEndpoint(endpoint);
    }

    @Test
    public void shouldSendQNEWhenPublishingEndpoints() throws Exception {
        final RestApi api = createRestApi("name", "path");
        local.publish(api);

        Message qne = assertMesageSent(Message.Type.QNE, local.getAgent().getIden());
        assertEquals(api, ((QnePayload)qne.getData()).getApis().iterator().next());
    }


    @Test
    public void shouldSendPresenceWhenRegisterMsnosApiInOrderToUpdateRemoteAgentEndpoints() throws Exception {
        final RestApi api = createMsnosApi("24.24.24.24");
        ensureLocalNetworkPresent(asPublicNetwork(api.getHost()));

        local.publish(api);
        
        assertMesageSent(Message.Type.PRS, local.getAgent().getIden());
    }

    @Test 
    public void shouldProcessExternalMessage() throws MsnosException {
        RemoteMicroservice remote = simulateRemoteMicroserviceJoin(randomUUID(), "24.24.24.24", "name", createRestApi("name", "/foo"));

        Message message = newAPPMessage(remote, cloud);
        microcloud.process(message);
        
        verify(cloud).process(message);
    }
    
    @Test 
    public void shouldEnquiryUponReceivingMessagesFromUnknownMicroservices() throws Exception {
        RestApi restApi = createRestApi("name", "/foo");
        Endpoint ep = new BaseEndpoint(Type.UDP, asPublicNetwork("24.24.24.24"));
        RemoteAgent agent = new RemoteAgent(randomUUID(), cloud, asSet(ep));
        RemoteMicroservice remote = new RemoteMicroservice("name", agent, toSet(restApi));

        simulateMessageFromCloud(newAPPMessage(remote, cloud));
        
        Message message = sentMessages().get(0);
        assertEquals(Message.Type.ENQ, message.getType());
        assertEquals(agent.getIden(), message.getTo());
    }
    
    @Test 
    public void shouldNOTEnquiryUponReceivingMessagesFromCloud() throws Exception {
        RemoteAgent agent = new RemoteAgent(randomUUID(), cloud, Collections.<Endpoint>emptySet() );
        RemoteMicroservice remote = new RemoteMicroservice("name", agent, Collections.<RestApi>emptySet());

        simulateMessageFromCloud(newHCKMessage(remote, true));
        
        assertEquals(0, sentMessages().size());
    }
    
    @Test 
    public void shouldNOTEnquiryUponReceivingLeavingPresenceFromAgent() throws Exception {
        RemoteAgent agent = new RemoteAgent(randomUUID(), cloud, Collections.<Endpoint>emptySet() );

        simulateMessageFromCloud(newLeaveMessage(agent));
        
        assertEquals(0, sentMessages().size());
    }
    
    private Message assertMesageSent(final Message.Type type, final Iden iden) throws MsnosException {
        for (Message message : sentMessages()) {
            if (message.getType() == type && message.getFrom().equals(iden))
                return message;
        }
        
        fail("Message of type "+type+" from "+iden+" not found!");
        return null;
    }

    private List<Message> sentMessages() throws MsnosException {
        try {
            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(cloud, atLeastOnce()).send(captor.capture());
            List<Message> messages = captor.getAllValues();
            return messages;
        } catch (Throwable ex) {
            return Collections.emptyList();
        }
    }

    private void ensureLocalNetworkPresent(Network network) {
        Endpoint endpoint = new BaseEndpoint(Type.UDP, network);
        when(local.getAgent().getEndpoints()).thenReturn(asSet(endpoint));
    }

    private RemoteMicroservice setupRemoteMicroservice(String host, String name, String apiPath) {
        short port = 9999;
        RestApi restApi = new RestApi(name, apiPath, port).onHost(host);
        return simulateRemoteMicroserviceJoin(UUID.randomUUID(), host, name, restApi);
    }

    private RemoteMicroservice simulateRemoteMicroserviceJoin(UUID uuid, String host, String name, RestApi restApi) {
        Endpoint ep = new BaseEndpoint(Type.UDP, asPublicNetwork(host));
        RemoteAgent agent = new RemoteAgent(uuid, cloud, asSet(ep));
        putRemoteAgentInCloudAgentsList(agent);

        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(restApi));
        simulateMessageFromCloud(newQNEMessage(remote.getAgent(), name, restApi));
        return remote;
    }

    private Message simulateMessageFromCloud(final Message message) {
        ArgumentCaptor<Cloud.Listener> cloudListener = ArgumentCaptor.forClass(Cloud.Listener.class);
        verify(cloud, atLeastOnce()).addListener(cloudListener.capture());
        for (Listener listener : cloudListener.getAllValues()) {
            listener.onMessage(message);
                       
        }
        return message;
    }

    private RestApi getFirstRestApi(RemoteMicroservice remote) {
        Set<RestApi> apis = remote.getApis();
        return apis.iterator().next();
    }

    private RestApi createRestApi(String name, String path) {
        return new RestApi(name, path, 9999).onHost("24.24.24.24");
    }

    private RestApi createMsnosApi(String host) {
        return new RestApi("foo", "/to/path", 9999, host, RestApi.Type.MSNOS_HTTP, false);
    }

    private RemoteEntity newRemoteAgentWithFakeHosts(String address) throws Exception {
        List<String> tokens = Arrays.asList(address.split("\\."));
        byte[] nibbles = new byte[tokens.size()];
        for (int i = 0; i < nibbles.length; i++) {
            nibbles[i] = Byte.valueOf(tokens.get(i));
        }
        
        return newRemoteAgent(UUID.randomUUID(), new BaseEndpoint(Type.UDP, new Network(nibbles, (short)1)));
    }

    private RemoteAgent newRemoteAgent() {
        return newRemoteAgent(UUID.randomUUID());
    }

    private RemoteAgent newRemoteAgent(final UUID uuid, BaseEndpoint... endpoints) {
        RemoteAgent remote = new RemoteAgent(uuid, cloud, new HashSet<Endpoint>(Arrays.asList(endpoints)));
        putRemoteAgentInCloudAgentsList(remote);
        return remote;
    }

    private void putRemoteAgentInCloudAgentsList(final RemoteAgent agent) {
        when(cloud.getRemoteAgents()).thenReturn(new HashSet<RemoteAgent>(Arrays.asList(agent)));
        when(cloud.find(agent.getIden())).thenReturn(agent);
    }

    private void assertAgentInMicroserviceList(Agent remoteAgent) {
        assertEquals(remoteAgent, microcloud.getMicroServices().iterator().next().getAgent());
    }

    private Set<RestApi> toSet(RestApi... restApi) {
        return new HashSet<RestApi>(Arrays.asList(restApi));
    }

}
