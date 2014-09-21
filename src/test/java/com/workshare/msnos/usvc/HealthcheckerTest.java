package com.workshare.msnos.usvc;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;

@SuppressWarnings("restriction")
public class HealthcheckerTest {

    private Healthchecker healthchecker;
    private ScheduledExecutorService scheduler;
    private Microcloud microcloud;
    private HttpServer httpServer;

    @Before
    public void setUp() throws Exception {
        microcloud = mock(Microcloud.class);
        
        scheduler = mock(ScheduledExecutorService.class);
        healthchecker = new Healthchecker(microcloud, scheduler);
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
        RemoteAgent agent = mock(RemoteAgent.class);
        RestApi alfa = new RestApi(name, endpoint, 9999).onHost(host1).asHealthCheck();
        RestApi beta = new RestApi(name, endpoint, 9999).onHost(host2);
        RestApi thre = new RestApi(name, endpoint, 9999).onHost(host3);
        RestApi four = new RestApi(name, endpoint, 9999).onHost(host4);
        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(alfa, beta, thre, four));
        when(microcloud.getMicroServices()).thenReturn(Arrays.asList(remote));
        return remote;
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
}
