package com.workshare.msnos.core.routing;

import static com.workshare.msnos.core.Message.Type.TRC;
import static com.workshare.msnos.core.cloud.CoreHelper.asPublicNetwork;
import static com.workshare.msnos.core.cloud.CoreHelper.createMockCloud;
import static com.workshare.msnos.core.cloud.CoreHelper.newAPPMesage;
import static com.workshare.msnos.core.cloud.CoreHelper.newAgentIden;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Identifiable;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.core.cloud.LocalAgent;
import com.workshare.msnos.core.cloud.MessageValidators;
import com.workshare.msnos.core.payloads.TracePayload;
import com.workshare.msnos.core.payloads.TracePayload.Crumb;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.www.WWWGateway;
import com.workshare.msnos.core.receipts.SingleReceipt;

public abstract class RouterAbstractTest {
    
    private static final Network PUBLIC_HOST = asPublicNetwork("25.25.25.25");
    
    protected static final int MAXIMUM_HOPS_DIRECT = 11;
    protected static final int MAXIMUM_HOPS_CLOUD = 3;
    protected static final int MAXIMUM_MESSAGES_PER_RING = 2;

    private Router router;

    protected Cloud cloud;
    
    protected Ring asia;
    protected RemoteAgent asiaOne;
    protected RemoteAgent asiaTwo;

    protected Ring europe;
    protected RemoteAgent europeOne;
    protected RemoteAgent europeTwo;

    protected Ring usa;
    protected RemoteAgent usaOne;
    protected RemoteAgent usaTwo;
    protected RemoteAgent usaTre;
    protected RemoteAgent usaFor;

    protected UDPGateway udp;
    protected HttpGateway http;
    protected WWWGateway www;

    protected LocalAgent self;
    protected MessageValidators validators;

    private Set<RemoteAgent> cloudAgents;


    @Before
    public void beforeEachTest() throws IOException {
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

        cloud = createMockCloud();
        cloudAgents = new HashSet<RemoteAgent>();
        when(cloud.getRemoteAgents()).thenReturn(cloudAgents);

        validators = mock(MessageValidators.class);
        when(validators.isForwardable(any(Message.class))).thenReturn(MessageValidators.SUCCESS);
        when(cloud.validators()).thenReturn(validators );
        
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

        System.setProperty(Router.SYSP_MAXIMUM_HOPS_DIRECT, Integer.toString(MAXIMUM_HOPS_DIRECT));
        System.setProperty(Router.SYSP_MAXIMUM_HOPS_CLOUD, Integer.toString(MAXIMUM_HOPS_CLOUD));
        System.setProperty(Router.SYSP_MAXIMUM_MESSAGES_PER_RING, Integer.toString(MAXIMUM_MESSAGES_PER_RING));
    }
    
    protected abstract Receipt process(Message message) throws IOException
    ;

    @Test
    public void shouldNotProcessMessagesWithZeroHops() throws Exception {
        Message message = newAPPMesage(usaOne, asiaTwo).make().withHops(0);

        process(message);

        verifyZeroInteractions(udp, http, www);
    }

    @Test
    public void shouldGoViaUDPBroadcastWithZeroHopsIfTargetIsInMyRing() throws Exception {
        Message message = newAPPMesage(asiaOne, europeTwo).withHops(10).make();
        
        process(message);
        
        assertSentOnlyViaUDP(message, 0);
    }

    @Test
    public void shouldGoViaHTTPWithZeroHopsIfTargetIsConnectedToMe() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        Message message = newAPPMesage(asiaOne, usaTwo).withHops(10).make();
        
        process(message);
        
        assertSentOnlyViaHTTP(message, 0, usaTwo);
    }
    
    @Test
    public void shouldGoViaHTTPWithOneHopIfTargetRingIsConnectedToMe() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        Message message = newAPPMesage(asiaOne, usaOne).withHops(10).make();
        
        process(message);
        
        assertSentOnlyViaHTTP(message, 1, usaTwo);
    }

    @Test
    public void shouldGoViaUDPOnWithZeroHopsIfTargetInMyRingAndNotConnectedToMe() throws Exception {
        connecMyselfViaHTTPTo(europeTwo);
        Message message = newAPPMesage(asiaOne, europeOne).withHops(10).make();
        
        process(message);

        assertSentOnlyViaUDP(message, 0);
    }
    
    @Test
    public void shouldGoViaUDPBroadcastWithMaximumHopsIfNoConnectionAtAll() throws Exception {
        Message message = newAPPMesage(asiaOne, usaOne).withHops(10).make();
        
        process(message);
    
        assertSentOnlyViaUDP(message, MAXIMUM_HOPS_DIRECT);
    }

    @Test
    public void shouldGoViaUDPBroadcastWithMaximumHopsIfTargetUnknown() throws Exception {
        Message message = newAPPMesage(asiaOne.getIden(), newAgentIden()).withHops(10).make();
        
        process(message);

        assertSentOnlyViaUDP(message, MAXIMUM_HOPS_DIRECT);
    }

    @Test
    public void shouldGoViaWWWBroadcastWithUnchangedHops() throws Exception {
        Message message = newAPPMesage(asiaOne, europeTwo).withHops(7).make();
    
        process(message);
    
        assertSentViaWWW(message, message.getHops());
    }
   
    @Test
    public void shouldCloudMessageGoViaUDPBroadcastWithMaximumHopsIfNoConnectionAtAll() throws Exception {
        Message message = newAPPMesage(asiaOne, cloud).withHops(10).make();
        
        process(message);

        assertSentOnlyViaUDP(message, MAXIMUM_HOPS_CLOUD);
    }

    @Test
    public void shouldCloudMessageGoViaUDPBroadcastAndHTTPViaRingWithMaximumHopsIfConnectedToRings() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        connecMyselfViaHTTPTo(asiaTwo);
        Message message = newAPPMesage(europeOne, cloud).withHops(10).make();
        
        process(message);

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
        
        router().forward(message);

        assertEquals(MAXIMUM_MESSAGES_PER_RING, anyMessagesOn(http).size());
    }
    
    @Test
    public void shouldFallbackToUDPIfTargetIsConnectedToMeButSendFails() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        Message message = newAPPMesage(asiaOne, usaTwo).withHops(10).make();
        when(http.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(SingleReceipt.failure(message));
        
        router().forward(message);
        
        assertSentViaUDP(message, MAXIMUM_HOPS_DIRECT);
    }
    
    @Test
    public void shouldFallbackToHTTPViaRingIfTargetIsConnectedToMeButSendFails() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        connecMyselfViaHTTPTo(usaTre);
        Message message = newAPPMesage(asiaOne, usaTwo).withHops(10).make();
        when(http.send(any(Cloud.class), any(Message.class), eq(usaTwo))).thenReturn(SingleReceipt.failure(message));
        
        router().forward(message);
        
        assertSentViaHTTP(message, 1, usaTre);
    }
    
    @Test
    public void shouldFallbackToUDPIfTargetIsConnectedToMeButAllHttpSendFail() throws Exception {
        connecMyselfViaHTTPTo(usaTwo);
        connecMyselfViaHTTPTo(usaTre);
        Message message = newAPPMesage(asiaOne, usaTwo).withHops(10).make();
        when(http.send(any(Cloud.class), any(Message.class), any(Identifiable.class))).thenReturn(SingleReceipt.failure(message));
        
        router().forward(message);
        
        assertSentViaUDP(message, MAXIMUM_HOPS_DIRECT);
    }

    @Test
    public void shouldSentTraceMessageCrumbed() throws Exception {
        Message message = new MessageBuilder(TRC, usaOne, asiaTwo).withHops(10).with(new TracePayload(newAgentIden())).make();

        process(message);

        message = allMessages().iterator().next();
        List<Crumb> crumbs = ((TracePayload)message.getData()).crumbs();
        assertEquals(1, crumbs.size());
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
    
    private void connecMyselfViaHTTPTo(RemoteAgent other) {
        Set<Endpoint> points = new HashSet<Endpoint>();
        points.add(new HttpEndpoint(PUBLIC_HOST, "http://url", other.getIden()));
        when(other.getEndpoints(eq(Endpoint.Type.HTTP))).thenReturn(points );
    }

    protected Router router() {
        if (router == null) {
            router = new Router(cloud, udp, http, www);
        }
        
        return router;
    }

    protected void assertSentOnlyViaUDP(final Message message, final int hops) throws IOException {
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
    
    protected void assertSentViaWWW(Message message, int hops) throws IOException {
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

    protected List<Message> anyMessagesOn(final Gateway gate) throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(gate, atMost(9999)).send(any(Cloud.class), captor.capture(), any(Identifiable.class));
        return captor.getAllValues();
    }
    
    protected Collection<Message> allMessages() throws IOException {
        Map<UUID, Message> messages = new HashMap<UUID, Message>();
        
        Gateway[] gates = new Gateway[]{udp, http, www};
        for (Gateway gate: gates) {
            for (Message message : anyMessagesOn(gate)) {
                if (!messages.containsKey(message.getUuid()))
                    messages.put(message.getUuid(), message);
            }
        }
        
        return messages.values();
    }
}
