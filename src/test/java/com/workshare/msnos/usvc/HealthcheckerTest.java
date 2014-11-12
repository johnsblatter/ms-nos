package com.workshare.msnos.usvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import java.util.UUID;
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
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;

@SuppressWarnings("restriction")
public class HealthcheckerTest {

    private static final int HTTP_PORT = 19999;
    
    private Healthchecker healthchecker;
    private ScheduledExecutorService scheduler;
    private Microcloud microcloud;
    private HttpServer httpServer;

    @Before
    public void setUp() throws Exception {
        Cloud cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(newIden());
  
        microcloud = mock(Microcloud.class);
        when(microcloud.getCloud()).thenReturn(cloud);
        
        scheduler = mock(ScheduledExecutorService.class);
        healthchecker = new Healthchecker(microcloud, scheduler);
        httpServer = null;
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
    public void shouldOnIOExceptionMarkAllApisFaulty() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroservice();
        setupNOHealthcheck();

        startAndRunCheck();

        assertTrue(allApisFaulty(remote));
    }

    @Test
    public void shouldOnFaultyHealthCheckMarkAllApisFaulty() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroservice();
        setupHealthcheck(500);

        startAndRunCheck();

        assertTrue(allApisFaulty(remote));
    }

    @Test
    public void shouldOn200HealthCheckMarkAllApisWorking() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroservice();
        setupHealthcheck(200);

        startAndRunCheck();

        assertTrue(allApisWorking(remote));
    }

    @Test
    public void shouldSendENQToMicroservicesPeriodically() throws Exception {
        fakeSystemTime(100000);
        RemoteMicroservice remote = setupRemoteMicroservice();
        setupHealthcheck(200);
        healthchecker = new Healthchecker(microcloud, scheduler);
        healthchecker.start();

        fakeSystemTime(SystemTime.asMillis()+2*Healthchecker.ENQ_PERIOD);
        runCheck();

        Message message = getLastMessageSent();
        assertNotNull(message);
        assertEquals(Message.Type.ENQ, message.getType());
        assertEquals(remote.getAgent().getIden(), message.getTo());
    }
    
    @Test
    public void shouldNotSendENQToMicroservicesIfLastEnquiryTimeNotElapsed() throws Exception {
        fakeSystemTime(100000);
        RemoteMicroservice remote = setupRemoteMicroservice();
        setupHealthcheck(200);
        healthchecker = new Healthchecker(microcloud, scheduler);
        healthchecker.start();

        fakeSystemTime(100000+(int)(Healthchecker.ENQ_PERIOD*0.75));
        remote.setApis(new HashSet<RestApi>());

        fakeSystemTime(100000+(int)(Healthchecker.ENQ_PERIOD*1.25));
        runCheck();

        Message message = getLastMessageSent();
        assertNull(message);
    }
    
        
    protected void startAndRunCheck() {
        healthchecker.start();
        runCheck();
    }

    private void setupHealthcheck(final int code) throws IOException {
        setupNOHealthcheck();
        HttpContext context = httpServer.createContext("/content/files/");
        context.setHandler(new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                httpExchange.sendResponseHeaders(code, -1);
            }
        });
    }

    protected HttpServer setupNOHealthcheck() throws IOException {
        return setUpHttpServer("127.0.0.1", HTTP_PORT);
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

    private Message getLastMessageSent() {
        try {
            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(microcloud, atLeastOnce()).send(captor.capture());
            return captor.getValue();
        } catch (Throwable any) {
            return null;
        }
    }

    private void runCheck() {
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

    private RemoteMicroservice setupRemoteMicroservice() throws Exception {
        return setupRemoteMicroserviceMultipleAPIsAndHealthCheck("127.0.0.1", "10.10.10.25", "10.10.10.91", "10.10.10.143", "content", "files");
    }

    private RemoteMicroservice setupRemoteMicroserviceMultipleAPIsAndHealthCheck(String host1, String host2, String host3, String host4, String name, String endpoint) throws Exception {
        RemoteAgent agent = mock(RemoteAgent.class);
        when(agent.getIden()).thenReturn(newIden());

        RestApi alfa = new RestApi(name, endpoint, HTTP_PORT).onHost(host1).asHealthCheck();
        RestApi beta = new RestApi(name, endpoint, HTTP_PORT).onHost(host2);
        RestApi thre = new RestApi(name, endpoint, HTTP_PORT).onHost(host3);
        RestApi four = new RestApi(name, endpoint, HTTP_PORT).onHost(host4);
        RemoteMicroservice remote = new RemoteMicroservice(name, agent, toSet(alfa, beta, thre, four));
        when(microcloud.getMicroServices()).thenReturn(Arrays.asList(remote));
        return remote;
    }

    private Set<RestApi> toSet(RestApi... restApi) {
        return new HashSet<RestApi>(Arrays.asList(restApi));
    }

    protected Iden newIden() {
        Iden iden = new Iden(Iden.Type.CLD, UUID.randomUUID());
        return iden;
    }

    private void fakeSystemTime(final long time) {
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return time;
            }
        });
    }
}
