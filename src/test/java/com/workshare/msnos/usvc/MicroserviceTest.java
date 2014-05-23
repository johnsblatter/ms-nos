package com.workshare.msnos.usvc;

import com.workshare.msnos.core.*;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.time.SystemTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class MicroserviceTest {

    private Cloud cloud;
    private Microservice localMicroservice;

    @Before
    public void prepare() throws Exception {
        cloud = Mockito.mock(Cloud.class);
        Mockito.when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, new UUID(111, 111)));

        localMicroservice = getLocalMicroservice();

        fakeSystemTime(12345L);
    }

    @After
    public void after() {
        SystemTime.reset();
    }

    @Test
    public void shouldInternalAgentJoinTheCloudOnJoin() throws Exception {
        localMicroservice = new Microservice("jeff");
        cloud = new Cloud(UUID.randomUUID(), Collections.<Gateway>emptySet());

        localMicroservice.join(cloud);

        assertEquals(localMicroservice.getAgent(), cloud.getLocalAgents().iterator().next());
    }

    @Test
    public void shouldSendQNEwhenPublishApi() throws Exception {
        RestApi api = new RestApi("/foo", 8080);
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

        simulateMessageFromCloud(getFaultMessage(remoteMicroservice.getAgent()));

        assertFalse(localMicroservice.getMicroServices().contains(remoteMicroservice));
    }

    @Test
    public void shouldSendQNEOnEnquiry() throws Exception {
        RemoteMicroservice remoteMicroservice = setupRemoteMicroservice("remote", "/endpoint");

        simulateMessageFromCloud(getENQMessage(remoteMicroservice.getAgent(), localMicroservice.getAgent()));
        Message msg = getLastMessageSent();

        assertEquals(Message.Type.QNE, msg.getType());
        assertEquals(cloud.getIden(), msg.getTo());
    }

    @Test
    public void shouldNotProcessMessagesFromSelf() throws Exception {
        simulateMessageFromCloud(getENQMessage(localMicroservice.getAgent(), localMicroservice.getAgent()));

        Message msg = getLastMessageSent();

        assertEquals(Message.Type.ENQ, msg.getType());
    }

    @Test
    public void shouldReturnCorrectRestApi() throws Exception {
        String endpoint = "/files";

        setUpFourRemoteMicroservicesTwoWithSpecifiedEndpoint(endpoint);

        RestApi result = localMicroservice.searchApi("content", endpoint);

        assertFalse(result.getPath().equals("/folders"));
        assertTrue(endpoint.equals(result.getPath()));
    }

    @Test
    public void shouldBeAbleToMarkRestApiAsFaulty() throws Exception {
        String endpoint = "/files";
        setUpFourRemoteMicroservicesTwoWithSpecifiedEndpoint(endpoint);

        RestApi result1 = localMicroservice.searchApi("content", endpoint);
        result1.markAsFaulty();

        RestApi result2 = localMicroservice.searchApi("content", endpoint);
        assertFalse(result2.equals(result1));
    }

    @Test
    public void shouldReturnNullWhenNoWorkingMicroserviceAvailable() throws Exception {
        String name = "name";
        String endpoint = "/users";
        setupRemoteMicroservice(name, endpoint);

        RestApi result1 = localMicroservice.searchApi(name, endpoint);
        result1.markAsFaulty();

        RestApi result2 = localMicroservice.searchApi(name, endpoint);
        assertNull(result2);
    }

    @Test
    public void shouldCreateRemoteMicroserviceOnQNE() throws IOException {
        RemoteAgent remoteAgent = newRemoteAgent();

        simulateMessageFromCloud(newQNEMessage(remoteAgent.getIden()));

        assertAgentInMicroserviceList(remoteAgent);
    }

    @Test
    public void shouldCreateBoundRestApisWhenRestApiNotBound() throws Exception {
        RemoteAgent remoteAgent = newRemoteAgentWithFakeHosts("10.10.10.10", (short) 15);

        RestApi unboundApi = new RestApi("/files", 9999);
        simulateMessageFromCloud(newQNEMessage(remoteAgent.getIden(), unboundApi));

        RestApi api = getRestApi();
        assertEquals(api.getHost(), "10.10.10.10/15");
    }

    @Test
    public void shouldFollowSelectionAlgorithmWhenRestApiMarkedAsFaulty() throws Exception {
        setupRemoteMicroserviceWithMultipleRestAPIs("25.25.25.25", "15.15.10.1", "content", "/files");
        setupRemoteMicroserviceWithHost("10.10.10.10", "content", "/files");

        RestApi result1 = localMicroservice.searchApi("content", "/files");
        result1.markAsFaulty();

        RestApi result2 = localMicroservice.searchApi("content", "/files");
        assertEquals("10.10.10.10", result2.getHost());

        RestApi result3 = localMicroservice.searchApi("content", "/files");
        assertEquals("15.15.10.1", result3.getHost());
    }

    private void setUpFourRemoteMicroservicesTwoWithSpecifiedEndpoint(String endpoint) throws IOException {
        setupRemoteMicroservice("content", endpoint);
        setupRemoteMicroservice("content", endpoint);
        setupRemoteMicroservice("peoples", "/users");
        setupRemoteMicroservice("content", "/folders");
    }

    private void putRemoteAgentInCloudAgentsList(RemoteAgent agent) {
        Mockito.when(cloud.getRemoteAgents()).thenReturn(new HashSet<RemoteAgent>(Arrays.asList(agent)));
    }

    private RestApi getRestApi() {
        RemoteMicroservice remote = localMicroservice.getMicroServices().get(0);
        Set<RestApi> apis = remote.getApis();
        return apis.iterator().next();
    }

    private void assertAgentInMicroserviceList(Agent remoteAgent) {
        assertEquals(remoteAgent, localMicroservice.getMicroServices().iterator().next().getAgent());
    }

    private Message newQNEMessage(Iden iden, RestApi... apis) {
        return new Message(Message.Type.QNE, iden, cloud.getIden(), 2, false, new QnePayload("content", apis));
    }

    private Microservice getLocalMicroservice() throws IOException {
        Microservice uService1 = new Microservice("fluffy");
        uService1.join(cloud);
        return uService1;
    }

    private RemoteMicroservice setupRemoteMicroserviceWithHost(String host, String name, String endpoint) {
        return getRemoteMicroservice(host, name, endpoint);
    }

    private RemoteMicroservice setupRemoteMicroservice(String name, String endpoint) throws IOException {
        return getRemoteMicroservice("10.10.10.10", name, endpoint);
    }

    private RemoteMicroservice getRemoteMicroservice(String host, String name, String endpoint) {
        RemoteAgent agent = newRemoteAgent();
        RestApi restApi = new RestApi(endpoint, 9999).host(host);
        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(restApi));
        return addRemoteAgentToCloudListAndMicroserviceToLocalList(name, remote, restApi);
    }

    private RemoteMicroservice setupRemoteMicroserviceWithMultipleRestAPIs(String host1, String host2, String name, String endpoint) throws IOException {
        RemoteAgent agent = newRemoteAgent();
        RestApi restApi = new RestApi(endpoint, 9999).host(host1);
        RestApi restApi2 = new RestApi(endpoint, 9999).host(host2);
        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(restApi, restApi2));
        return addRemoteAgentToCloudListAndMicroserviceToLocalList(name, remote, restApi, restApi2);
    }

    private RemoteMicroservice addRemoteAgentToCloudListAndMicroserviceToLocalList(String name, RemoteMicroservice remote, RestApi... restApi) {
        putRemoteAgentInCloudAgentsList(remote.getAgent());
        simulateMessageFromCloud(new Message(Message.Type.QNE, remote.getAgent().getIden(), cloud.getIden(), 2, false, new QnePayload(name, restApi)));
        return remote;
    }

    private Set<RestApi> toSet(RestApi... restApi) {
        return new HashSet<RestApi>(Arrays.asList(restApi));
    }

    private Message getENQMessage(Identifiable from, Identifiable to) {
        return new Message(Message.Type.ENQ, from.getIden(), to.getIden(), 2, false, null);
    }

    private Message getFaultMessage(Agent agent) {
        return new Message(Message.Type.FLT, cloud.getIden(), cloud.getIden(), 2, false, new FltPayload(agent.getIden()));
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
        return newRemoteAgent(UUID.randomUUID(), new Network(nibbles, suffix));
    }

    private RemoteAgent newRemoteAgent() {
        return newRemoteAgent(UUID.randomUUID());
    }

    private RemoteAgent newRemoteAgent(final UUID uuid, Network... hosts) {
        RemoteAgent remote = new RemoteAgent(uuid, cloud, new HashSet<Network>(Arrays.asList(hosts)));
        putRemoteAgentInCloudAgentsList(remote);
        return remote;
    }
}
