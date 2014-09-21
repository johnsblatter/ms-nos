package com.workshare.msnos.usvc;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Identifiable;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MockMessageHelper;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.RemoteEntity;
import com.workshare.msnos.core.cloud.JoinSynchronizer;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint.Type;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.routing.strategies.CachingRoutingStrategy;
import com.workshare.msnos.usvc.api.routing.strategies.PriorityRoutingStrategy;

@SuppressWarnings("unused")
public class MicroserviceTest {

    private static final long CURRENT_TIME = 12345L;
    private Cloud cloud;
    private Microcloud microcloud;
    private Microservice localMicroservice;

    @BeforeClass
    public static void disableCaching() {
        System.setProperty(CachingRoutingStrategy.SYSP_TIMEOUT, "0");
    }

    @Before
    public void prepare() throws Exception {
        cloud = Mockito.mock(Cloud.class);
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, new UUID(111, 111)));
        microcloud = newMicrocloud(cloud);
        
        localMicroservice = new Microservice("fluffy");
        localMicroservice.join(microcloud);

        fakeSystemTime(CURRENT_TIME);
    }

    @After
    public void after() {
        System.setProperty(PriorityRoutingStrategy.SYSP_PRIORITY_ENABLED, "true");
        SystemTime.reset();
    }

    @Test
    public void shouldInternalAgentJoinTheCloudOnJoin() throws Exception {
        localMicroservice = new Microservice("jeff");
        cloud = new Cloud(UUID.randomUUID(), " ", mockGateways(), mock(JoinSynchronizer.class), null);

        localMicroservice.join(newMicrocloud(cloud));

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
    public void shouldMarkWorkingTouchTheUnderlyingAgent() throws Exception {
        RemoteMicroservice service = setupRemoteMicroservice("content", "/files");

        final long now = 987654L;
        fakeSystemTime(now);
        service.markWorking();

        assertEquals(now, service.getAgent().getAccessTime());
    }

    @Test
    public void shouldPublishRestApisWithHighPriorityWhenSet() throws Exception {
        System.setProperty(PriorityRoutingStrategy.SYSP_PRIORITY_DEFAULT_LEVEL, "5");

        localMicroservice.publish(new RestApi("test", "path", 9999));

        assertEquals(5, getLastPublishedRestApi().getPriority());
    }

    private RestApi getLastPublishedRestApi() throws IOException {
        return ((QnePayload) getLastMessageSent().getData()).getApis().iterator().next();
    }

    private RestApi createRestApi(String name, String endpoint) {
        return new RestApi(name, endpoint, 9999).onHost("24.24.24.24");
    }

    private Set<RestApi> getApis(RemoteMicroservice remoteMicroservice) {
        return microcloud.getMicroServices().get(microcloud.getMicroServices().indexOf(remoteMicroservice)).getApis();
    }

    private RestApi[] getRestApis(RemoteMicroservice ms1) {
        Set<RestApi> apiSet = ms1.getApis();
        return apiSet.toArray(new RestApi[apiSet.size()]);
    }

    private void putRemoteAgentInCloudAgentsList(final RemoteAgent agent) {
        when(cloud.getRemoteAgents()).thenReturn(new HashSet<RemoteAgent>(Arrays.asList(agent)));
        when(cloud.find(agent.getIden())).thenReturn(agent);
    }

    private RestApi getRestApi() {
        RemoteMicroservice remote = microcloud.getMicroServices().get(0);
        Set<RestApi> apis = remote.getApis();
        return apis.iterator().next();
    }

    private void assertAgentInMicroserviceList(Agent remoteAgent) {
        assertEquals(remoteAgent, microcloud.getMicroServices().iterator().next().getAgent());
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

    private Message newQNEMessage(RemoteEntity from, String name, RestApi... apis) {
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

    private RemoteEntity newRemoteAgentWithFakeHosts(String address, short suffix) throws Exception {
        List<String> tokens = Arrays.asList(address.split("\\."));
        if (tokens.size() > 4) throw new Exception("Split too large, not correct network address");
        byte[] nibbles = new byte[tokens.size()];
        for (int i = 0; i < nibbles.length; i++) {
            nibbles[i] = Byte.valueOf(tokens.get(i));
        }
        return newRemoteAgent(UUID.randomUUID(), new BaseEndpoint(Type.UDP, new Network(nibbles, suffix)));
    }

    private RemoteAgent newRemoteAgent() {
        return newRemoteAgent(UUID.randomUUID());
    }

    private RemoteAgent newRemoteAgentWithUUID(UUID uuid) {
        return newRemoteAgent(uuid);
    }

    private RemoteAgent newRemoteAgent(final UUID uuid, BaseEndpoint... endpoints) {
        RemoteAgent remote = new RemoteAgent(uuid, cloud, new HashSet<Endpoint>(Arrays.asList(endpoints)));
        putRemoteAgentInCloudAgentsList(remote);
        return remote;
    }
    
    private Microcloud newMicrocloud(Cloud cloud) {
        return new Microcloud(cloud, mock(ScheduledExecutorService.class));
    }


}
