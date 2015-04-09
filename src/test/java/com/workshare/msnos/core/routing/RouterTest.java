package com.workshare.msnos.core.routing;

import static com.workshare.msnos.core.CoreHelper.asPublicNetwork;
import static com.workshare.msnos.core.CoreHelper.asSet;
import static com.workshare.msnos.core.CoreHelper.newAPPMesage;
import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static com.workshare.msnos.core.CoreHelper.newCloudIden;
import static com.workshare.msnos.core.CoreHelper.sleep;
import static com.workshare.msnos.core.GatewaysHelper.newHttpGateway;
import static com.workshare.msnos.core.GatewaysHelper.newUDPGateway;
import static com.workshare.msnos.core.GatewaysHelper.newWWWGateway;
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
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Identifiable;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.SingleReceipt;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.www.WWWGateway;

public class RouterTest {
    
    private static final Network PUBLIC_HOST = asPublicNetwork("25.25.25.25");

    private static final long EXPIRE_TIME = 100L;
    private static final int MAXIMUM_HOPS_DIRECT = 11;
    private static final int MAXIMUM_HOPS_CLOUD = 3;
    private static final int MAXIMUM_MESSAGES_PER_RING = 2;

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
    private WWWGateway www;

    private LocalAgent self;

    private Set<RemoteAgent> cloudAgents;

    @Before
    public void before() throws IOException {
        Receipt receipt = mock(Receipt.class);
        when(receipt.getStatus()).thenReturn(Message.Status.DELIVERED);
        
        udp = mock(UDPGateway.class);
        when(udp.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(receipt);
        when(udp.name()).thenReturn("UDP");

        http = mock(HttpGateway.class);
        when(http.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(receipt);
        when(http.name()).thenReturn("HTTP");

        www = mock(WWWGateway.class);
        when(www.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(receipt);
        when(www.name()).thenReturn("WWW");

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
        System.setProperty(Router.SYSP_MAXIMUM_HOPS_DIRECT, Integer.toString(MAXIMUM_HOPS_DIRECT));
        System.setProperty(Router.SYSP_MAXIMUM_HOPS_CLOUD, Integer.toString(MAXIMUM_HOPS_CLOUD));
        System.setProperty(Router.SYSP_MAXIMUM_MESSAGES_PER_RING, Integer.toString(MAXIMUM_MESSAGES_PER_RING));
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
    public void shouldPickuppGateways() throws Exception {
        HttpGateway http = newHttpGateway();
        UDPGateway udp = newUDPGateway();
        WWWGateway www = newWWWGateway();
        Set<Gateway> gates = asSet(http, udp, www);

        Router router = new Router(cloud, gates);
        
        assertEquals(udp, router.udpGateway());
        assertEquals(http, router.httpGateway());
        assertEquals(www, router.wwwGateway());
    }
    
    @Test
    public void shouldNotRouteMessagesWithZeroHops() throws Exception {
        Message message = newAPPMesage(usaOne, asiaTwo).make().withHops(0);
        
        router().process(message);
        
        verifyZeroInteractions(udp, http, www);
    }

    @Test
    public void shouldNotRouteAlreadyRoutedMessages() throws Exception {
        Message message = newAPPMesage(usaOne, asiaTwo).make();
        
        router().process(message);
        router().process(message);
        
        assertEquals(1, anyMessagesOn(udp).size() + anyMessagesOn(http).size());
    }

    @Test
    public void shouldProcessedMessagesCacheExpire() throws Exception {
        Message message = newAPPMesage(usaOne, asiaTwo).make();
        
        router().process(message);
        sleep(2*EXPIRE_TIME, MILLISECONDS);
        router().process(message);
        
        assertEquals(2, anyMessagesOn(udp).size() + anyMessagesOn(http).size());
    }

    @Test
    public void shouldNotMarkAsRoutedMessagesFailedToBeDelivered() throws Exception {
        Message message = newAPPMesage(usaOne, asiaTwo).make();
        when(udp.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(SingleReceipt.failure(message));
        
        router().process(message);
        router().process(message);
        
        assertEquals(2, anyMessagesOn(udp).size() + anyMessagesOn(http).size());
    }

    @Test
    public void shouldGoViaWWWBroadcastWithUnchangedHops() throws Exception {
        Message message = newAPPMesage(asiaOne, europeTwo).withHops(7).make();
        
        router().process(message);
        
        assertSentViaWWW(message, message.getHops());
    }
    

    @Test
    public void shouldGoViaUDPBroadcastWithZeroHopsIfTargetIsInMyRing() throws Exception {
        Message message = newAPPMesage(asiaOne, europeTwo).withHops(10).make();
        
        router().process(message);
        
        assertSentOnlyViaUDP(message, 0);
    }
    

    @Test
    public void shouldGoViaHTTPWithZeroHopsIfTargetIsConnectedToMe() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        Message message = newAPPMesage(asiaOne, usaTwo).withHops(10).make();
        
        router().process(message);
        
        assertSentOnlyViaHTTP(message, 0, usaTwo);
    }
    
    @Test
    public void shouldGoViaHTTPWithOneHopIfTargetRingIsConnectedToMe() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        Message message = newAPPMesage(asiaOne, usaOne).withHops(10).make();
        
        router().process(message);
        
        assertSentOnlyViaHTTP(message, 1, usaTwo);
    }

    @Test
    public void shouldGoViaUDPOnMyRingWithZeroHopsIfNotConnectedToMe() throws Exception {
        connecMyselfViaHTTPTo(europeTwo);
        Message message = newAPPMesage(asiaOne, europeOne).withHops(10).make();
        
        router().process(message);

        assertSentOnlyViaUDP(message, 0);
    }
    
    @Test
    public void shouldGoViaUDPBroadcastWithMaximumHopsIfNoConnectionAtAll() throws Exception {
        Message message = newAPPMesage(asiaOne, usaOne).withHops(10).make();
        
        router().process(message);

        assertSentOnlyViaUDP(message, MAXIMUM_HOPS_DIRECT);
    }

    
    @Test
    public void shouldGoViaUDPBroadcastWithMaximumHopsIfTargetUnknown() throws Exception {
        Message message = newAPPMesage(asiaOne.getIden(), newAgentIden()).withHops(10).make();
        
        router().process(message);

        assertSentOnlyViaUDP(message, MAXIMUM_HOPS_DIRECT);
    }
    
    @Test
    public void shouldCloudMessageGoViaUDPBroadcastWithMaximumHopsIfNoConnectionAtAll() throws Exception {
        Message message = newAPPMesage(asiaOne, cloud).withHops(10).make();
        
        router().process(message);

        assertSentOnlyViaUDP(message, MAXIMUM_HOPS_CLOUD);
    }

    @Test
    public void shouldCloudMessageGoViaUDPBroadcastAndHTTPViaRingWithMaximumHopsIfConnectedToRings() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        connecMyselfViaHTTPTo(asiaTwo);
        Message message = newAPPMesage(europeOne, cloud).withHops(10).make();
        
        router().process(message);

        assertSentViaUDP(message, MAXIMUM_HOPS_CLOUD);
        assertSentViaHTTP(message, MAXIMUM_HOPS_CLOUD, usaTwo);
        assertSentViaHTTP(message, MAXIMUM_HOPS_CLOUD, asiaTwo);
        assertEquals(2, anyMessagesOn(http).size());
    }


    @Test
    public void shouldCloudMessageGoViaHTTPViaRingWithMaximumHopsIfConnectedToRingsWithMaximumTwoAgents() throws Exception {
        connecMyselfViaHTTPTo(usaOne);
        connecMyselfViaHTTPTo(usaTwo);
        connecMyselfViaHTTPTo(usaTre);
        connecMyselfViaHTTPTo(usaFor);
        Message message = newAPPMesage(europeOne, cloud).withHops(10).make();
        
        router().process(message);

        assertEquals(MAXIMUM_MESSAGES_PER_RING, anyMessagesOn(http).size());
    }

    // TODO FIXME
    // describe what happens when send() fails - if not able to send via HTTP, should we reroute it?
    
    @Test
    public void shouldFallbackToUDPIfTargetIsConnectedToMeButSendFails() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        Message message = newAPPMesage(asiaOne, usaTwo).withHops(10).make();
        when(http.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(SingleReceipt.failure(message));
        
        router().process(message);
        
        assertSentViaUDP(message, MAXIMUM_HOPS_DIRECT);
    }
    
    @Test
    public void shouldFallbackToHTTPViaRingUDPIfTargetIsConnectedToMeButSendFails() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        connecMyselfViaHTTPTo(usaTre);
        Message message = newAPPMesage(asiaOne, usaTwo).withHops(10).make();
        when(http.send(any(Cloud.class), any(Message.class), eq(usaTwo))).thenReturn(SingleReceipt.failure(message));
        
        router().process(message);
        
        assertSentViaHTTP(message, 1, usaTre);
    }
    
    @Test
    public void shouldFallbackToUDPIfTargetIsConnectedToMeButAllHttpSendFail() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        connecMyselfViaHTTPTo(usaTre);
        Message message = newAPPMesage(asiaOne, usaTwo).withHops(10).make();
        when(http.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(SingleReceipt.failure(message));
        
        router().process(message);
        
        assertSentViaUDP(message, MAXIMUM_HOPS_DIRECT);
    }
    
    private void connecMyselfViaHTTPTo(RemoteAgent other) {
        Set<Endpoint> points = new HashSet<Endpoint>();
        points.add(new HttpEndpoint(PUBLIC_HOST, "http://url", other.getIden()));
        when(other.getEndpoints(eq(Endpoint.Type.HTTP))).thenReturn(points );
    }

    private Router router() {
        if (router == null)
            router = new Router(cloud, udp, http, www);

        return router;
    }

    private void assertSentOnlyViaUDP(final Message message, final int hops) throws IOException {
        verifyZeroInteractions(http);
        assertSentViaUDP(message, hops);
        assertEquals(1, anyMessagesOn(udp).size());
    }

    private void assertSentViaUDP(final Message message, final int hops) throws IOException {
        final Message sent = findMessageOrFail(messagesOn(udp, null), message);
        assertEquals(hops, sent.getHops());
    }

    private void assertSentOnlyViaHTTP(Message message, int hops, Identifiable to) throws IOException {
        verifyZeroInteractions(udp);
        assertSentViaHTTP(message, hops, to);
        assertEquals(1, anyMessagesOn(http).size());
    }

    private void assertSentViaHTTP(Message message, int hops, Identifiable to) throws IOException {
        Message sent = findMessageOrFail(messagesOn(http, to), message);
        assertEquals(hops, sent.getHops());
    }
    
    private void assertSentViaWWW(Message message, int hops) throws IOException {
        Message sent = findMessageOrFail(messagesOn(www, null), message);
        assertEquals(hops, sent.getHops());
    }
    

    private Message findMessageOrFail(List<Message> messages, Message tofind) {
        for (Message message : messages) {
            if (message.getUuid().equals(tofind.getUuid()))
                return message;
        }

        throw new AssertionError("Message not found!");
    }

    private List<Message> messagesOn(final Gateway gate, Identifiable to) throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(gate, atMost(9999)).send(any(Cloud.class), captor.capture(), eq(to));
        return captor.getAllValues();
    }

    private List<Message> anyMessagesOn(final Gateway gate) throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(gate, atMost(9999)).send(any(Cloud.class), captor.capture(), any(Identifiable.class));
        return captor.getAllValues();
    }
}
