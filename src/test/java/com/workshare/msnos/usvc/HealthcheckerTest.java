package com.workshare.msnos.usvc;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings("restriction")
public class HealthcheckerTest {

    private Healthchecker healthchecker;
    private ScheduledExecutorService scheduler;
    private Cloud cloud;
    private HttpServer httpServer;

    @Before
    public void setUp() throws Exception {
        cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, UUID.randomUUID()));
        
        scheduler = mock(ScheduledExecutorService.class);
        Microservice microservice = getLocalMicroservice();
        healthchecker = new Healthchecker(microservice, scheduler);
        httpServer = null;
        fakeSystemTime(12345L);
    }

    @After
    public void tearDown() throws Exception {
        SystemTime.reset();
    }

    @After
    public void stopHttpServer() throws Exception {
        if (httpServer != null)
            httpServer.stop(0);
    }

    @Test
    public void shouldOnFaultyHealthCheckMarkAllApisFaulty() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroserviceMultipleAPIsAndHealthCheck("127.0.0.1", "10.10.10.25", "10.10.10.91", "10.10.10.143", "content", "files");
        setUpHttpServer("127.0.0.1", 9999);

        healthchecker.run();
        fakeSystemTime(99999999L);
        forceRunCloudPeriodicCheck();

        assertTrue(allApisFaulty(remote));
    }


    @Test
    public void shouldOn200HealthCheckMarkAllApisWorking() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroserviceMultipleAPIsAndHealthCheck("127.0.0.1", "10.10.10.25", "10.10.10.91", "10.10.10.143", "content", "files");
        setupHttpServerWithHandlerResponds200();

        healthchecker.run();
        fakeSystemTime(99999L);
        forceRunCloudPeriodicCheck();

        assertTrue(allApisWorking(remote));
    }

    private void setupHttpServerWithHandlerResponds200() throws IOException {
        setUpHttpServer("127.0.0.1", 9999);
        HttpContext context = httpServer.createContext("/content/files/");
        context.setHandler(new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                httpExchange.sendResponseHeaders(200, 0);
            }
        });
    }

    private HttpServer setUpHttpServer(String host, int port) throws IOException {
        if (httpServer == null) {
            httpServer = HttpServer.create();
            httpServer.bind(new InetSocketAddress(host, port), port);
            httpServer.start();
        }
        return httpServer;
    }

    private Runnable capturePeriodicRunableCheck() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, atLeastOnce()).scheduleAtFixedRate(captor.capture(), anyInt(), anyInt(), any(TimeUnit.class));
        return captor.getValue();
    }

    private void forceRunCloudPeriodicCheck() {
        Runnable runnable = capturePeriodicRunableCheck();
        runnable.run();
    }

    private boolean allApisWorking(RemoteMicroservice remote) {
        for (RestApi rest : remote.getApis()) {
            if (rest.isFaulty()) return false;
        }
        return true;
    }

    private boolean allApisFaulty(RemoteMicroservice remote) {
        for (RestApi rest : remote.getApis()) {
            if (!rest.isFaulty()) return false;
        }
        return true;
    }

    private RemoteMicroservice setupRemoteMicroserviceMultipleAPIsAndHealthCheck(String host1, String host2, String host3, String host4, String name, String endpoint) throws Exception {
        RemoteAgent agent = newRemoteAgent();
        RestApi alfa = new RestApi(name, endpoint, 9999).onHost(host1).asHealthCheck();
        RestApi beta = new RestApi(name, endpoint, 9999).onHost(host2);
        RestApi thre = new RestApi(name, endpoint, 9999).onHost(host3);
        RestApi four = new RestApi(name, endpoint, 9999).onHost(host4);
        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(alfa, beta, thre, four));
        return addRemoteAgentToCloudListAndMicroserviceToLocalList(name, remote, alfa, beta, thre, four);
    }

    private RemoteMicroservice addRemoteAgentToCloudListAndMicroserviceToLocalList(String name, RemoteMicroservice remote, RestApi... restApi) {
        putRemoteAgentInCloudAgentsList(remote.getAgent());
        final Message message = new MessageBuilder(Message.Type.QNE, remote.getAgent(), cloud).with(new QnePayload(name, restApi)).make();
        simulateMessageFromCloud(message);
        return remote;
    }

    private RemoteAgent newRemoteAgent() {
        RemoteAgent remote = new RemoteAgent(UUID.randomUUID(), cloud, Collections.<Network>emptySet());
        putRemoteAgentInCloudAgentsList(remote);
        return remote;
    }

    private void putRemoteAgentInCloudAgentsList(RemoteAgent agent) {
        Mockito.when(cloud.getRemoteAgents()).thenReturn(new HashSet<RemoteAgent>(Arrays.asList(agent)));
    }

    private Message simulateMessageFromCloud(final Message message) {
        ArgumentCaptor<Cloud.Listener> cloudListener = ArgumentCaptor.forClass(Cloud.Listener.class);
        verify(cloud, atLeastOnce()).addListener(cloudListener.capture());
        cloudListener.getValue().onMessage(message);
        return message;
    }

    private Set<RestApi> toSet(RestApi... restApi) {
        return new HashSet<RestApi>(Arrays.asList(restApi));
    }


    private void fakeSystemTime(final long time) {
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return time;
            }
        });
    }

    private Microservice getLocalMicroservice() throws IOException {
        Microservice uService1 = new Microservice("fluffy");
        uService1.join(cloud);
        return uService1;
    }
}
