package com.workshare.msnos.usvc;

import static com.workshare.msnos.core.CoreHelper.asSet;
import static com.workshare.msnos.core.CoreHelper.fakeSystemTime;
import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static com.workshare.msnos.core.CoreHelper.newCloudIden;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.payloads.HealthcheckPayload;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;

@SuppressWarnings("restriction")
public class HealthcheckerTest {

    private static final String PATH = "files";

    private static final String NAME = "content";

    private static final int HTTP_PORT = 19999;
    
    private Healthchecker healthchecker;
    private ScheduledExecutorService scheduler;
    private Microcloud microcloud;
    private HttpServer httpServer;

    @Before
    public void setUp() throws Exception {
        Cloud cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(newCloudIden());
  
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
    public void shouldOn200HealthSendHCKMessageToCloud() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroservice();
        setupHealthcheck(200);

        startAndRunCheck();

        assertHealthcheckMessageSent(remote, true);
    }

    @Test
    public void shouldOnFaultyHealthCheckSendHCKMessageToCloud() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroservice();
        setupHealthcheck(500);

        startAndRunCheck();

        assertHealthcheckMessageSent(remote, false);
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

        Message message = getLastMessageSent(Message.Type.ENQ);
        assertNotNull(message);
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

        Message message = getLastMessageSent(Message.Type.ENQ);
        assertNull(message);
    }
    
    @Test
    public void shouldSkipRecentlyCheckedServices() throws Exception {
        RemoteMicroservice remote = setupRemoteMicroservice();
        when(remote.getLastChecked()).thenReturn(123456L);
        healthchecker = new Healthchecker(microcloud, scheduler);
        healthchecker.start();

        fakeSystemTime(remote.getLastChecked()+1000);
        runCheck();

        verify(remote, never()).markFaulty();
        verify(remote, never()).markWorking();
    }
    
    @Test
    public void shouldOn200HealthCheckMarkAllApisWorkingWheMultipleChecksAndOneFailingO() throws Exception {
        RestApi checkOk = new RestApi(NAME ,PATH, HTTP_PORT).onHost("127.0.0.1").asHealthCheck();
        RestApi checkKO = new RestApi(NAME, PATH, HTTP_PORT).onHost("0.0.0.0").asHealthCheck();
        RemoteMicroservice remote = setupRemoteMicroservice("name", checkOk, checkKO);
        setupHealthcheck(200);

        startAndRunCheck();

        assertTrue(allApisWorking(remote));
    }

    @Test
    public void shouldBeHealhtIfNoHealchecks() throws IOException {
        RestApi checkOk = new RestApi(NAME ,PATH, HTTP_PORT).onHost("127.0.0.1");
        RestApi checkKO = new RestApi(NAME, PATH, HTTP_PORT).onHost("0.0.0.0");
        RemoteMicroservice remote = setupRemoteMicroservice("name", checkOk, checkKO);

        startAndRunCheck();

        assertTrue(allApisWorking(remote));
    }
    
    protected void startAndRunCheck() {
        healthchecker.start();
        runCheck();
    }

    private void setupHealthcheck(final int code) throws IOException {
        setupNOHealthcheck();
        HttpContext context = httpServer.createContext("/");
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

    private Message getLastMessageSent(Message.Type typeOf) {
        List<Message> messages = getLastMessagesSent();
        for (Message message : messages) {
            if (typeOf.equals(message.getType()))
                return message;
        }
        
        return null;
    }

    private List<Message> getLastMessagesSent() {
        try {
            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(microcloud, atLeastOnce()).send(captor.capture());
            return captor.getAllValues();
        } catch (Throwable any) {
            return Collections.emptyList();
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
        return setupRemoteMicroserviceMultipleAPIsAndHealthCheck(NAME, PATH, "127.0.0.1", "10.10.10.25", "10.10.10.91", "10.10.10.143");
    }

    private void assertHealthcheckMessageSent(RemoteMicroservice remote, final boolean working) {
        Message message = getLastMessageSent(Message.Type.HCK);
        assertNotNull(message);
        assertEquals(microcloud.getCloud().getIden(), message.getTo());
        HealthcheckPayload payload = (HealthcheckPayload)message.getData();
        assertEquals(working, payload.isWorking());
        assertEquals(remote.getAgent().getIden(), payload.getIden());
    }

    private RemoteMicroservice setupRemoteMicroserviceMultipleAPIsAndHealthCheck(String name, String endpoint, String host1, String host2, String host3, String host4) throws Exception {
        RestApi alfa = new RestApi(name, endpoint, HTTP_PORT).onHost(host1).asHealthCheck();
        RestApi beta = new RestApi(name, endpoint, HTTP_PORT).onHost(host2);
        RestApi thre = new RestApi(name, endpoint, HTTP_PORT).onHost(host3);
        RestApi four = new RestApi(name, endpoint, HTTP_PORT).onHost(host4);

        return setupRemoteMicroservice(name, alfa, beta, thre, four);
    }

    private RemoteMicroservice setupRemoteMicroservice(String name, RestApi... apis) {
        RemoteAgent agent = mock(RemoteAgent.class);
        when(agent.getIden()).thenReturn(newAgentIden());

        RemoteMicroservice remote = spy(new RemoteMicroservice(name, agent, asSet(apis)));
        when(remote.getLastChecked()).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                return 0l;  // force check every time
            }});
        
        when(microcloud.getMicroServices()).thenReturn(Arrays.asList(remote));
        return remote;
    }
}
