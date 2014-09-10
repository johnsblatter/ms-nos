package com.workshare.msnos.usvc;

import com.workshare.msnos.core.*;
import com.workshare.msnos.core.cloud.JoinSynchronizer;
import com.workshare.msnos.core.cloud.Multicaster;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.security.Signer;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.strategies.CachingRoutingStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
public class MicroserviceTest {

    private static final long CURRENT_TIME = 12345L;
    private Cloud cloud;
    private Microservice localMicroservice;

    @BeforeClass
    public static void disableCaching() {
        System.setProperty(CachingRoutingStrategy.SYSP_TIMEOUT, "0");
    }

    @Before
    public void prepare() throws Exception {
        cloud = Mockito.mock(Cloud.class);
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, new UUID(111, 111)));
        when(cloud.generateNextMessageUUID()).thenReturn(UUID.randomUUID());

        fakeSystemTime(12345L);
        localMicroservice = new Microservice("fluffy");
        localMicroservice.join(cloud);
        fakeSystemTime(CURRENT_TIME);
    }

    @After
    public void after() {
        System.clearProperty("high.priority.mode");
        SystemTime.reset();
    }

    @Test
    public void shouldInternalAgentJoinTheCloudOnJoin() throws Exception {
        localMicroservice = new Microservice("jeff");
        cloud = new Cloud(UUID.randomUUID(), " ", new Signer(), mockGateways(), mock(JoinSynchronizer.class), mock(Multicaster.class), mock(ScheduledExecutorService.class));

        localMicroservice.join(cloud);

        assertEquals(localMicroservice.getAgent(), cloud.getLocalAgents().iterator().next());
    }

    private Set<Gateway> mockGateways() throws IOException {
        Gateway gate = mock(Gateway.class);
        Receipt receipt = mock(Receipt.class);
        when(gate.send(any(Cloud.class), any(Message.class))).thenReturn(receipt);
        return new HashSet<Gateway>(Arrays.asList(new Gateway[]{gate}));
    }

    @Test
    public void shouldSendQNEwhenPublishApi() throws Exception {
        RestApi api = new RestApi("test", "/foo", 8080);
        localMicroservice.publish(api);

        Message msg = getLastMessageSent();
        assertEquals(Message.Type.QNE, msg.getType());
        assertEquals(cloud.getIden(), msg.getTo());

        Set<RestApi> apis = ((QnePayload) msg.getData()).getApis();
        assertTrue(api.equals(apis.iterator().next()));
    }

    @Test
    public void shouldSendENQonJoin() throws Exception {
        Message msg = getLastMessageSent();

        assertEquals(Message.Type.ENQ, msg.getType());
        assertEquals(cloud.getIden(), msg.getTo());
    }

    @Test
    public void shouldBeRemovedWhenUnderlyingAgentDies() throws Exception {
        RemoteMicroservice remoteMicroservice = setupRemoteMicroservice("remote", "/endpoint");

        simulateMessageFromCloud(newFaultMessage(remoteMicroservice.getAgent()));

        assertFalse(localMicroservice.getMicroServices().contains(remoteMicroservice));
    }

    @Test
    public void shouldSendQNEOnEnquiry() throws Exception {
        RemoteMicroservice remoteMicroservice = setupRemoteMicroservice("remote", "/endpoint");

        simulateMessageFromCloud(newENQMessage(remoteMicroservice.getAgent(), localMicroservice.getAgent()));
        Message msg = getLastMessageSent();

        assertEquals(Message.Type.QNE, msg.getType());
        assertEquals(cloud.getIden(), msg.getTo());
    }

    @Test
    public void shouldNotProcessMessagesFromSelf() throws Exception {
        simulateMessageFromCloud(newENQMessage(localMicroservice.getAgent(), localMicroservice.getAgent()));

        Message msg = getLastMessageSent();

        assertEquals(Message.Type.ENQ, msg.getType());
    }

    @Test
    public void shouldReturnCorrectRestApi() throws Exception {
        String endpoint = "/files";

        setupRemoteMicroservice("content", endpoint);
        setupRemoteMicroservice("content", "/folders");

        RestApi result = localMicroservice.searchApi("content", endpoint);

        assertFalse(result.getPath().equals("/folders"));
        assertTrue(endpoint.equals(result.getPath()));
    }

    @Test
    public void shouldBeAbleToMarkRestApiAsFaulty() throws Exception {
        String endpoint = "/files";

        setupRemoteMicroservice("content", endpoint);
        setupRemoteMicroservice("content", endpoint);

        RestApi result1 = localMicroservice.searchApi("content", endpoint);
        result1.markFaulty();

        RestApi result2 = localMicroservice.searchApi("content", endpoint);
        assertFalse(result2.equals(result1));
    }

    @Test
    public void shouldReturnNullWhenNoWorkingMicroserviceAvailable() throws Exception {
        String name = "name";
        String endpoint = "/users";
        setupRemoteMicroservice(name, endpoint);

        RestApi result1 = localMicroservice.searchApi(name, endpoint);
        result1.markFaulty();

        RestApi result2 = localMicroservice.searchApi(name, endpoint);
        assertNull(result2);
    }

    @Test
    public void shouldCreateRemoteMicroserviceOnQNE() throws IOException {
        RemoteAgent remoteAgent = newRemoteAgent();

        simulateMessageFromCloud(newQNEMessage(remoteAgent, "content"));

        assertAgentInMicroserviceList(remoteAgent);
    }

    @Test
    public void shouldCreateBoundRestApisWhenRestApiNotBound() throws Exception {
        RemoteAgent remoteAgent = newRemoteAgentWithFakeHosts("10.10.10.10", (short) 15);

        RestApi unboundApi = new RestApi("test", "/test", 9999);
        simulateMessageFromCloud(newQNEMessage(remoteAgent, "content", unboundApi));

        RestApi api = getRestApi();
        assertEquals(api.getHost(), "10.10.10.10");
    }

    @Test
    public void shouldRoundRobinWhenSessionAffinityNotEnabled() throws Exception {
        RestApi api1 = getRestApis(setupRemoteMicroservice("24.24.24.24", "content", "/files"))[0];
        RestApi api2 = getRestApis(setupRemoteMicroservice("11.11.11.11", "content", "/folders"))[0];
        RestApi api3 = getRestApis(setupRemoteMicroservice("23.23.23.23", "content", "/files"))[0];
        RestApi api4 = getRestApis(setupRemoteMicroservice("25.22.22.22", "content", "/files"))[0];

        assertEquals(api1, localMicroservice.searchApi("content", "/files"));
        assertEquals(api3, localMicroservice.searchApi("content", "/files"));
        assertEquals(api4, localMicroservice.searchApi("content", "/files"));
    }

    @Test
    public void shouldRespondWithSameRemoteWhenSessionAffinityEnabled() throws Exception {
        setupRemoteMicroserviceWithSessionAffinity("11.11.11.11", "users", "/peoples");
        RestApi api1 = getRestApis(setupRemoteMicroserviceWithSessionAffinity("24.24.24.24", "content", "/files"))[0];
        setupRemoteMicroservice("23.23.23.23", "content", "/files");

        assertEquals(api1, localMicroservice.searchApi("content", "/files"));
        assertEquals(api1, localMicroservice.searchApi("content", "/files"));
    }

    @Test
    public void shouldNOTReturnFaultyApisWhenSessionAffinityEnabled() throws Exception {
        RestApi api1 = getRestApis(setupRemoteMicroserviceWithSessionAffinity("24.24.24.24", "content", "/files"))[0];
        setupRemoteMicroservice("25.25.25.25", "content", "/files");

        api1.markFaulty();

        assertFalse(api1.equals(localMicroservice.searchApi("content", "/files")));
    }

    @Test
    public void shouldMapNewAffinityWhenOriginalFaulty() throws Exception {
        RestApi api1 = getRestApis(setupRemoteMicroserviceWithSessionAffinity("24.24.24.24", "content", "/files"))[0];
        RestApi api2 = getRestApis(setupRemoteMicroserviceWithSessionAffinity("25.25.25.25", "content", "/files"))[0];

        assertEquals(api1, localMicroservice.searchApi("content", "/files"));

        api1.markFaulty();
        assertEquals(api2, localMicroservice.searchApi("content", "/files"));

        api1.markWorking();
        assertEquals(api2, localMicroservice.searchApi("content", "/files"));
    }

    // candidate for removal
    // FIXME  // TODO
    public void shouldFollowSelectionAlgorithmWhenRestApiMarkedAsFaulty() throws Exception {
        setupRemoteMicroserviceWithMultipleRestAPIs("25.25.25.25", "15.15.10.1", "content", "/files");
        setupRemoteMicroservice("10.10.10.10", "content", "/files");

        RestApi result1 = localMicroservice.searchApi("content", "/files");
        result1.markFaulty();

        RestApi result2 = localMicroservice.searchApi("content", "/files");
        assertEquals("10.10.10.10", result2.getHost());

        RestApi result3 = localMicroservice.searchApi("content", "/files");
        assertEquals("15.15.10.1", result3.getHost());
    }

    @Test
    public void shouldRemoveRemoteApisWhenRemoteMicroserviceIsRemoved() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroservice("24.24.24.24", "content", "/files");
        RestApi api1 = getRestApis(remote)[0];

        assertEquals(api1, localMicroservice.searchApi("content", "/files"));

        simulateMessageFromCloud(newFaultMessage(remote.getAgent()));

        assertNull(localMicroservice.searchApi("content", "/files"));
    }

    @Test
    public void shouldReturnCorrectApiWhenSearchingById() throws Exception {
        RestApi api1 = getRestApis(setupRemoteMicroservice("24.24.24.24", "content", "/files"))[0];
        RestApi api2 = getRestApis(setupRemoteMicroservice("11.11.11.11", "content", "/folders"))[0];
        RestApi api3 = getRestApis(setupRemoteMicroservice("23.23.23.23", "content", "/files"))[0];
        RestApi api4 = getRestApis(setupRemoteMicroservice("25.22.22.22", "content", "/files"))[0];

        assertEquals(api1, localMicroservice.searchApiById(api1.getId()));
        assertEquals(api2, localMicroservice.searchApiById(api2.getId()));
        assertEquals(api4, localMicroservice.searchApiById(api4.getId()));
    }

    @Test
    public void shouldReturnNullIfApiListIsEmpty() throws Exception {
        assertNull(localMicroservice.searchApi("something", "don't care"));
        assertNull(localMicroservice.searchApiById(1033l));
    }

    @Test
    public void shouldNOTSelectApisExposedBySelf() throws Exception {
        localMicroservice.publish(new RestApi("test", "alfa", 1234));
        RemoteMicroservice remoteMicroservice = setupRemoteMicroservice("test", "alfa");

        assertEquals(getRestApis(remoteMicroservice)[0], localMicroservice.searchApi("test", "alfa"));
    }

    @Test
    public void shouldUpdateMicroserviceIfPresent() throws Exception {
        UUID uuid = new UUID(11, 22);
        RemoteMicroservice remoteMicroservice = setupRemoteMicroserviceWithAgentUUIDAndRestApi("24.24.24.24", "content", "/files", uuid, createRestApi("content", "/files"));
        setupRemoteMicroserviceWithAgentUUIDAndRestApi("24.24.24.24", "content", "/files", uuid, createRestApi("content", "/healthcheck"));

        assertEquals(1, localMicroservice.getMicroServices().size());
        assertEquals(2, getApis(remoteMicroservice).size());
    }

    @Test
    public void shouldMarkWorkingTouchTheUnderlyingAgent() throws Exception {
        RemoteMicroservice service = setupRemoteMicroservice("content", "/files");

        final long now = 987654L;
        fakeSystemTime(now);
        service.markWorking();

        assertEquals(now, service.getAgent().getAccessTime());
    }

    @Test
    public void shouldPublishRestApisWithHighPriorityWhenSet() throws Exception {
        System.setProperty("high.priority.mode", "true");

        localMicroservice.publish(new RestApi("test", "path", 9999));

        assertEquals(RestApi.Priority.HIGH, getLastPublishedRestApi().getPriority());
    }

    private RestApi getLastPublishedRestApi() throws IOException {
        return ((QnePayload) getLastMessageSent().getData()).getApis().iterator().next();
    }

    private RestApi createRestApi(String name, String endpoint) {
        return new RestApi(name, endpoint, 9999).onHost("24.24.24.24");
    }

    private Set<RestApi> getApis(RemoteMicroservice remoteMicroservice) {
        return localMicroservice.getMicroServices().get(localMicroservice.getMicroServices().indexOf(remoteMicroservice)).getApis();
    }

    private RestApi[] getRestApis(RemoteMicroservice ms1) {
        Set<RestApi> apiSet = ms1.getApis();
        return apiSet.toArray(new RestApi[apiSet.size()]);
    }

    private void putRemoteAgentInCloudAgentsList(RemoteAgent agent) {
        when(cloud.getRemoteAgents()).thenReturn(new HashSet<RemoteAgent>(Arrays.asList(agent)));
    }

    private RestApi getRestApi() {
        RemoteMicroservice remote = localMicroservice.getMicroServices().get(0);
        Set<RestApi> apis = remote.getApis();
        return apis.iterator().next();
    }

    private void assertAgentInMicroserviceList(Agent remoteAgent) {
        assertEquals(remoteAgent, localMicroservice.getMicroServices().iterator().next().getAgent());
    }

    private RemoteMicroservice setupRemoteMicroservice(String name, String endpoint) throws IOException {
        return setupRemoteMicroservice("10.10.10.10", name, endpoint);
    }

    private RemoteMicroservice setupRemoteMicroserviceWithSessionAffinity(String host, String name, String endpoint) throws Exception {
        RemoteAgent agent = newRemoteAgent();
        RestApi restApi = new RestApi(name, endpoint, 9999).onHost(host).withAffinity();
        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(restApi));
        return addRemoteAgentToCloudListAndMicroserviceToLocalList(name, remote, restApi);
    }

    private RemoteMicroservice setupRemoteMicroservice(String host, String name, String endpoint) {
        return setupRemoteMicroservice(host, name, endpoint, 9999);
    }

    private RemoteMicroservice setupRemoteMicroservice(String host, String name, String endpoint, int port) {
        RemoteAgent agent = newRemoteAgent();
        RestApi restApi = new RestApi(name, endpoint, port).onHost(host);
        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(restApi));
        return addRemoteAgentToCloudListAndMicroserviceToLocalList(name, remote, restApi);

    }

    private RemoteMicroservice setupRemoteMicroserviceWithAgentUUIDAndRestApi(String host, String name, String endpoint, UUID uuid, RestApi restApi) {
        RemoteAgent agent = newRemoteAgentWithUUID(uuid);
        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(restApi));
        return addRemoteAgentToCloudListAndMicroserviceToLocalList(name, remote, restApi);

    }

    private RemoteMicroservice setupRemoteMicroserviceWithMultipleRestAPIs(String host1, String host2, String name, String endpoint) throws IOException {
        RemoteAgent agent = newRemoteAgent();
        RestApi alfa = new RestApi(name, endpoint, 9999).onHost(host1);
        RestApi beta = new RestApi(name, endpoint, 9999).onHost(host2);
        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(alfa, beta));
        return addRemoteAgentToCloudListAndMicroserviceToLocalList(name, remote, alfa, beta);
    }

    private RemoteMicroservice addRemoteAgentToCloudListAndMicroserviceToLocalList(String name, RemoteMicroservice remote, RestApi... restApi) {
        putRemoteAgentInCloudAgentsList(remote.getAgent());
        simulateMessageFromCloud(newQNEMessage(remote.getAgent(), name, restApi));
        return remote;
    }

    private Set<RestApi> toSet(RestApi... restApi) {
        return new HashSet<RestApi>(Arrays.asList(restApi));
    }

    private Message newENQMessage(Identifiable from, Identifiable to) {
        return new MockMessageHelper(Message.Type.ENQ, from.getIden(), to.getIden()).make();
    }

    private Message newQNEMessage(RemoteAgent from, String name, RestApi... apis) {
        return new MockMessageHelper(Message.Type.QNE, from.getIden(), cloud.getIden()).sequence(12).data(new QnePayload("content", apis)).make();
    }

    private Message newFaultMessage(Agent agent) {
        return new MessageBuilder(Message.Type.FLT, cloud, cloud).with(new FltPayload(agent.getIden())).make();
    }

    private Message getLastMessageSent() throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(cloud, atLeastOnce()).send(captor.capture());
        return captor.getValue();
    }

    private Message simulateMessageFromCloud(final Message message) {
        ArgumentCaptor<Cloud.Listener> cloudListener = ArgumentCaptor.forClass(Cloud.Listener.class);
        verify(cloud, atLeastOnce()).addListener(cloudListener.capture());
        cloudListener.getValue().onMessage(message);
        return message;
    }

    private void fakeSystemTime(final long time) {
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return time;
            }
        });
    }

    private RemoteAgent newRemoteAgentWithFakeHosts(String address, short suffix) throws Exception {
        List<String> tokens = Arrays.asList(address.split("\\."));
        if (tokens.size() > 4) throw new Exception("Split too large, not correct network address");
        byte[] nibbles = new byte[tokens.size()];
        for (int i = 0; i < nibbles.length; i++) {
            nibbles[i] = Byte.valueOf(tokens.get(i));
        }
        return newRemoteAgent(UUID.randomUUID(), new Endpoint(Type.UDP, new Network(nibbles, suffix)));
    }

    private RemoteAgent newRemoteAgent() {
        return newRemoteAgent(UUID.randomUUID());
    }

    private RemoteAgent newRemoteAgentWithUUID(UUID uuid) {
        return newRemoteAgent(uuid);
    }

    private RemoteAgent newRemoteAgent(final UUID uuid, Endpoint... endpoints) {
        RemoteAgent remote = new RemoteAgent(uuid, cloud, new HashSet<Endpoint>(Arrays.asList(endpoints)));
        putRemoteAgentInCloudAgentsList(remote);
        return remote;
    }
}
