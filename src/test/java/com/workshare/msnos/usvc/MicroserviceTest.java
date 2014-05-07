package com.workshare.msnos.usvc;

import com.workshare.msnos.core.*;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.soup.time.SystemTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class MicroserviceTest {

    Cloud cloud;
    Microservice uService1;
    Microservice otherMs;
    UDPGateway gate1;

    @Before
    public void prepare() throws Exception {
        cloud = Mockito.mock(Cloud.class);
        gate1 = Mockito.mock(UDPGateway.class);

        Iden iden = new Iden(Iden.Type.CLD, new UUID(111, 111));

        Mockito.when(cloud.getIden()).thenReturn(iden);

        uService1 = new Microservice("fluffy");
        otherMs = new Microservice("kiki");

        fakeSystemTime(12345L);
    }

    @After
    public void after() {
        SystemTime.reset();
    }

    @Test
    public void shouldInternalAgentJoinTheCloudOnJoin() throws Exception {
        cloud = new Cloud(UUID.randomUUID(), Collections.<Gateway>emptySet());
        uService1.join(cloud);
        assertEquals(uService1.getAgent(), cloud.getAgents().iterator().next());
    }

    @Test
    public void shouldSendQNEwhenPublishApi() throws Exception {
        uService1.join(cloud);

        RestApi api = new RestApi("/foo", 8080);
        uService1.publish(api);

        Message msg = getLastMessageSent();
        assertEquals(Message.Type.QNE, msg.getType());
        assertEquals(cloud.getIden(), msg.getTo());
        Set<RestApi> apis = ((QnePayload) msg.getData()).getApis();
        assertEquals(api, apis.iterator().next());
    }

    @Test
    public void shouldSendENQonJoin() throws Exception {
        uService1.join(cloud);

        Message msg = getLastMessageSent();

        assertEquals(Message.Type.ENQ, msg.getType());
        assertEquals(cloud.getIden(), msg.getTo());
    }

    @Test
    public void shouldProcessQNEMsgs() throws Exception {
        uService1.join(cloud);

        simulateMessageFromCloud(getQNEMessage(otherMs, uService1.getAgent()));

        assertTrue(iterateMicroServiceListGetByName(uService1, otherMs));
    }

    @Test
    public void shouldBeRemovedWhenUnderlyingAgentDies() throws Exception {
        uService1.join(cloud);
        Microservice remoteMicroservice = new Microservice("remote");

        simulateMessageFromCloud(getQNEMessage(remoteMicroservice, uService1.getAgent()));
        assertTrue(iterateMicroServiceListGetByName(uService1, remoteMicroservice));

        simulateMessageFromCloud(getFaultMessage(remoteMicroservice.getAgent()));

        assertFalse(iterateMicroServiceListGetByName(uService1, remoteMicroservice));
    }

    @Test
    public void shouldSendQNEOnEnquiry() throws Exception {
        uService1.join(cloud);
        Microservice remoteMicroservice = new Microservice("remote");

        simulateMessageFromCloud(getENQMessage(remoteMicroservice.getAgent(), uService1.getAgent()));

        Message msg = getLastMessageSent();

        assertEquals(Message.Type.QNE, msg.getType());
        assertEquals(cloud.getIden(), msg.getTo());
    }

    @Test
    public void shouldNotProcessMessagesFromSelf() throws Exception {
        uService1.join(cloud);

        simulateMessageFromCloud(getENQMessage(uService1.getAgent(), uService1.getAgent()));

        Message msg = getLastMessageSent();

        assertEquals(Message.Type.ENQ, msg.getType());
    }

    @Test
    public void shouldReturnCorrectRestApi() throws Exception {
        Microservice localMicroservice = getLocalMicroservice();
        String endpoint = "/files";

        setupRemoteMicroservice("content", endpoint);
        setupRemoteMicroservice("content", endpoint);
        setupRemoteMicroservice("peoples", "/users");
        setupRemoteMicroservice("content", "/folders");

        RestApi result = localMicroservice.searchApi("content", endpoint);

        assertFalse(result.getPath().equals("/folders"));
        assertTrue(endpoint.equals(result.getPath()));
    }

    @Test
    public void shouldBeAbleToMarkRestApiAsFaulty() throws Exception {
        Microservice localMicroservice = getLocalMicroservice();

        setupRemoteMicroservice("content", "/files");
        setupRemoteMicroservice("content", "/files");
        setupRemoteMicroservice("peoples", "/users");
        setupRemoteMicroservice("content", "/folders");

        RestApi result1 = localMicroservice.searchApi("content", "/files");
        result1.markAsFaulty();

        RestApi result2 = localMicroservice.searchApi("content", "/files");
        assertFalse(result2.equals(result1));
    }

    @Test
    public void shouldReturnNullWhenNoWorkingMicroserviceAvailable() throws Exception {
        Microservice localMicroservice = getLocalMicroservice();

        setupRemoteMicroservice("content", "/files");
        setupRemoteMicroservice("content", "/files");
        setupRemoteMicroservice("peoples", "/users");
        setupRemoteMicroservice("content", "/folders");

        RestApi result1 = localMicroservice.searchApi("peoples", "/users");
        result1.markAsFaulty();

        RestApi result2 = localMicroservice.searchApi("peoples", "/users");

        assertNull(result2);
    }

    private Microservice getLocalMicroservice() throws IOException {
        uService1.join(cloud);
        return uService1;
    }

    private boolean iterateMicroServiceListGetByName(Microservice ms1, Microservice ms2) {
        for (RemoteMicroservice remote : ms1.getMicroServices()) {
            if (remote.getName().equals(ms2.getName())) return true;
        }
        return false;
    }

    private Microservice setupRemoteMicroservice(String name, String endpoint) throws IOException {
        Microservice remote = new Microservice(name);
        simulateMessageFromCloud(new Message(Message.Type.QNE, remote.getAgent().getIden(), cloud.getIden(), 2, false, new QnePayload(name, new RestApi(endpoint, 9999))));
        return remote;
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

    private void simulateMessageFromCloud(final Message message) {
        ArgumentCaptor<Cloud.Listener> cloudListener = ArgumentCaptor.forClass(Cloud.Listener.class);
        verify(cloud, atLeastOnce()).addListener(cloudListener.capture());
        cloudListener.getValue().onMessage(message);
    }

    private Message getQNEMessage(Microservice from, Identifiable to) {
        return new Message(Message.Type.QNE, from.getAgent().getIden(), to.getIden(), 2, false, new QnePayload(from.getName(), new RestApi("/" + from.getName(), 222)));
    }

    private void fakeSystemTime(final long time) {
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return time;
            }
        });
    }
}
