package com.workshare.msnos.core.routing;

import static com.workshare.msnos.core.CoreHelper.asPublicNetwork;
import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static com.workshare.msnos.core.CoreHelper.newCloudIden;
import static com.workshare.msnos.core.CoreHelper.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.CoreHelper;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Identifiable;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.routing.Router;

@SuppressWarnings("unused")
public class RouterTest {
    
    private static final Network PUBLIC_HOST = asPublicNetwork("25.25.25.25");

    private static final long EXPIRE_TIME = 100L;
    private static final int MAXIMUM_HOPS = 11;

    private Router router;

    private Cloud cloud;
    
    private Ring asia;
    private RemoteAgent asiaOne;
    private RemoteAgent asiaTwo;

    private Ring europe;
    private RemoteAgent europeOne;
    private RemoteAgent europeTwo;

    private Ring usa;
    private RemoteAgent usaOne;
    private RemoteAgent usaTwo;
    private RemoteAgent usaTre;
    private RemoteAgent usaFor;

    private UDPGateway udp;
    private HttpGateway http;

    private LocalAgent self;

    private Set<RemoteAgent> cloudAgents;

    @Before
    public void before() throws IOException {
        Receipt receipt = mock(Receipt.class);
        
        udp = mock(UDPGateway.class);
        when(udp.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(receipt);
        when(udp.name()).thenReturn("UDP");

        http = mock(HttpGateway.class);
        when(http.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(receipt);
        when(http.name()).thenReturn("HTTP");

        cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(newCloudIden());
        when(cloud.getRing()).thenReturn(Ring.random());
        cloudAgents = new HashSet<RemoteAgent>();
        when(cloud.getRemoteAgents()).thenReturn(cloudAgents);
        
        usa = Ring.random();
        usaOne = installRemoteAgent(usa, "usaOne");
        usaTwo = installRemoteAgent(usa, "usaTwo");
        usaTre = installRemoteAgent(usa, "usaTre");
        usaFor = installRemoteAgent(usa, "usaFor");

        asia = Ring.random();
        asiaOne = installRemoteAgent(asia, "asiaOne");
        asiaTwo = installRemoteAgent(asia, "asiaTwo");

        europe = cloud.getRing();
        europeOne = installRemoteAgent(europe, "europeOne");
        europeTwo = installRemoteAgent(europe, "europetwo");
        
        self = mock(LocalAgent.class);
        when(self.getIden()).thenReturn(newAgentIden());
        when(self.getCloud()).thenReturn(cloud);
        when(self.getRing()).thenReturn(europe);

        System.setProperty(Router.SYSP_PROCESS_EXPIRE, Long.toString(EXPIRE_TIME));
        System.setProperty(Router.SYSP_MAXIMUM_HOPS, Integer.toString(MAXIMUM_HOPS));
    }
    
    private RemoteAgent installRemoteAgent(final Ring ring, final String name) {
        RemoteAgent remote = newRemoteAgent(ring);
        when(remote.toString()).thenReturn(name);
        when(cloud.getRemoteAgent(remote.getIden())).thenReturn(remote);
        cloudAgents.add(remote);
        return remote;
    }

    private RemoteAgent newRemoteAgent(Ring ring) {
        RemoteAgent remote = mock(RemoteAgent.class);
        when(remote.getCloud()).thenReturn(cloud);
        when(remote.getRing()).thenReturn(ring);
        when(remote.getIden()).thenReturn(newAgentIden());
        return remote;
    }

    @Test
    public void shouldNotRouteMessagesWithZeroHops() throws Exception {
        Message message = CoreHelper.newAPPMesage(usaOne, asiaTwo).make().withHops(0);
        
        router().process(message);
        
        assertEquals(0, messagesOn(udp).size() + messagesOn(http).size());
    }

    @Test
    public void shouldNotRouteAlreadyRoutedMessages() throws Exception {
        Message message = CoreHelper.newAPPMesage(usaOne, asiaTwo).make();
        
        router().process(message);
        router().process(message);
        
        assertEquals(1, messagesOn(udp).size() + messagesOn(http).size());
    }

    @Test
    public void shouldProcessedMessagesCacheExpire() throws Exception {
        Message message = CoreHelper.newAPPMesage(usaOne, asiaTwo).make();
        
        router().process(message);
        sleep(2*EXPIRE_TIME, MILLISECONDS);
        router().process(message);
        
        assertEquals(2, messagesOn(udp).size() + messagesOn(http).size());
    }

    @Test
    public void shouldGoViaUDPBroadcastWithZeroHopsIfTargetIsInMyRing() throws Exception {
        Message message = CoreHelper.newAPPMesage(asiaOne, europeTwo).withHops(10).make();
        
        router().process(message);
        
        assertSentOnlyViaUDP(message, 0);
    }
    

    @Test
    public void shouldGoViaHTTPWithZeroHopsIfTargetIsConnectedToMe() throws Exception {
        connecMyselfToViaHTTP(usaTwo);
        Message message = CoreHelper.newAPPMesage(asiaOne, usaTwo).withHops(10).make();
        
        router().process(message);
        
        assertSentOnlyViaHTTP(message, 0);
    }
    
    @Test
    public void shouldGoViaHTTPWithOneHopIfTargetRingIsConnectedToMe() throws Exception {
        connecMyselfToViaHTTP(usaTwo);
        Message message = CoreHelper.newAPPMesage(asiaOne, usaOne).withHops(10).make();
        
        router().process(message);
        
        assertSentOnlyViaHTTP(message, 1);
    }

    @Test
    public void shouldGoViaUDPOnMyRingWithZeroHopsIfNotConnectedToMe() throws Exception {
        connecMyselfToViaHTTP(europeTwo);
        Message message = CoreHelper.newAPPMesage(asiaOne, europeOne).withHops(10).make();
        
        router().process(message);

        assertSentOnlyViaUDP(message, 0);
    }
    
    @Test
    public void shouldGoViaUDPBroadcastWithMaximumHopsIfNoConnectionAtAll() throws Exception {
        Message message = CoreHelper.newAPPMesage(asiaOne, usaOne).withHops(10).make();
        
        router().process(message);

        assertSentOnlyViaUDP(message, MAXIMUM_HOPS);
    }

    
    @Test
    public void shouldGoViaUDPBroadcastWithMaximumHopsIfTargetUnknown() throws Exception {
        Message message = CoreHelper.newAPPMesage(asiaOne.getIden(), newAgentIden()).withHops(10).make();
        
        router().process(message);

        assertSentOnlyViaUDP(message, MAXIMUM_HOPS);
    }
    
    @Test
    public void shouldCloudMessageGoViaUDPBroadcastWithMaximumHopsIfNoConnectionAtAll() throws Exception {
        Message message = CoreHelper.newAPPMesage(asiaOne, cloud).withHops(10).make();
        
        router().process(message);

        assertSentOnlyViaUDP(message, MAXIMUM_HOPS);
    }

//    @Test
    public void shouldCloudMessageGoViaUDPBroadcastAndHTTPViaRingWithMaximumHopsIfConnectedToRings() throws Exception {
        connecMyselfToViaHTTP(usaTwo);
        connecMyselfToViaHTTP(asiaTwo);
        Message message = CoreHelper.newAPPMesage(europeOne, cloud).withHops(10).make();
        
        router().process(message);

        assertSentViaUDP(message, MAXIMUM_HOPS);

//        final Message sent = messagesOn(http);
//        assertMessageEquals(sent, message);
//        assertEquals(MAXIMUM_HOPS, sent.getHops());
    }


    // TODO FIXME
    // describe what happens when send() fails - if not able to send via HTTP, should we reroute it?
    
    private void connecMyselfToViaHTTP(RemoteAgent other) {
        Set<Endpoint> points = new HashSet<Endpoint>();
        points.add(new HttpEndpoint(PUBLIC_HOST, "http://url", other.getIden()));
        when(other.getEndpoints(eq(Endpoint.Type.HTTP))).thenReturn(points );
    }

    private void assertMessageEquals(Message expected, Message current) {        
        assertEquals(expected.getUuid(), current.getUuid());
    }

    private Router router() {
        if (router == null)
            router = new Router(cloud, udp, http);

        return router;
    }

    private void setRouter(Router router) {
        this.router = router;
    }

    private void assertSentOnlyViaUDP(final Message message, final int hops) throws IOException {
        verifyZeroInteractions(http);
        assertSentViaUDP(message, hops);
    }

    private void assertSentViaUDP(final Message message, final int hops) throws IOException {
        final Message sent = messagesOn(udp).get(0);
        assertMessageEquals(sent, message);
        assertEquals(hops, sent.getHops());
    }

    private void assertSentOnlyViaHTTP(final Message message, final int hops) throws IOException {
        verifyZeroInteractions(udp);
        assertSentViaHTTP(message, hops);
    }

    private void assertSentViaHTTP(final Message message, final int hops) throws IOException {
        final Message sent = messagesOn(http).get(0);
        assertMessageEquals(sent, message);
        assertEquals(hops, sent.getHops());
    }

    private List<Message> messagesOn(final Gateway gate) throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(gate, atMost(9999)).send(any(Cloud.class), captor.capture(), any(Identifiable.class));
        return captor.getAllValues();
    }

}
