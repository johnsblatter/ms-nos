package com.workshare.msnos.core.protocols.ip.http;

import static com.workshare.msnos.core.Iden.Type.AGT;
import static com.workshare.msnos.core.Iden.Type.CLD;
import static com.workshare.msnos.core.protocols.ip.Endpoint.Type.HTTP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MessageBuilder.Mode;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoints;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.core.protocols.ip.www.HttpClientHelper;
import com.workshare.msnos.soup.threading.Multicaster;

public class HttpGatewaytest {

    private HttpGateway gate;
    private Cloud cloud;
    private Multicaster<Listener, Message> caster;
    private HttpClientHelper http;

    @Before
    public void setup() throws Exception {
        
        http = new HttpClientHelper();

        caster = synchronousMulticaster();
        gate = new HttpGateway(http.client(), caster);

        cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(newIden(CLD));

    }

    @Test
    public void shouldReturnInstallableEndpoints() throws Exception {

        Endpoints endpoints = gate.endpoints();
        Endpoint ep1 = endpoints.install(Mockito.mock(Endpoint.class));
        
        assertTrue(endpoints.all().contains(ep1));
    }
    
    @Test
    public void shouldReturnRemovableEndpoints() throws Exception {

        Endpoints endpoints = gate.endpoints();
        Endpoint ep1 = endpoints.install(Mockito.mock(Endpoint.class));
        endpoints.remove(ep1);
        
        assertEquals(0, endpoints.all().size());
    }

    @Test
    public void shouldReturnFailedDeliveryReceiptWhenDirectedToCloud() throws Exception {
        Message message = newSampleMessage(AGT, CLD);
        Receipt receipt = gate.send(cloud, message);

        assertEquals(Message.Status.FAILED, receipt.getStatus());
    }
/*
    @Test
    public void shouldReturnFailedDeliveryReceiptWhenNoRouteToTarget() throws Exception {
        installEndpoint("25.25.25.25", 8080);
        installRemoteAgent("21.21.21.21", 9020);
        
//        Message message = newSampleMessage(newIden(AGT), new Iden(AGT, uuid));
        Receipt receipt = gate.send(cloud, message);

        assertEquals(Message.Status.FAILED, receipt.getStatus());
    }

    @Test
    public void shouldSendTheMessageUsingHttpClient() throws Exception {
        installEndpoint("25.25.25.25", 8080);

        Message message = newSampleMessage();
        gate.send(cloud, message);

        HttpPost request = http.getLastPostToWWW();
        assertNotNull(request);
//        assertEquals("http://192.168.0.1/messagesRequestUrl(cloud), request.getURI().toString());
//        assertEquals(toText(uuid1), toText(request.getEntity()));
    }

    private void installRemoteAgent(String host, int port) {
//        Collection<RemoteAgent> agents = cloud.getRemoteAgents();
//        
//        List<RemoteAgent> newAgents = new ArrayList<RemoteAgent>();
//        newAgents.add(```)
//        if (agents != null) newAgents.addAll(agents);
//        
    }
*/
    private void installEndpoint(final String host, final int port) throws MsnosException {
        Network net = newNetwork(host);
        final Endpoint endpoint = new Endpoint(HTTP, net, (short)port);
        gate.endpoints().install(endpoint);
    }
    
    private Iden newIden(final com.workshare.msnos.core.Iden.Type idenType) {
        return new Iden(idenType, UUID.randomUUID());
    }
    
    private Message newSampleMessage() {
        return newSampleMessage(AGT, AGT);
    }

    private Message newSampleMessage(com.workshare.msnos.core.Iden.Type from, com.workshare.msnos.core.Iden.Type to) {
        return newSampleMessage(newIden(from), newIden(to));
    }

    private Message newSampleMessage(Iden from, Iden to) {
        return new MessageBuilder(Mode.RELAXED, Type.APP, from, to).with(UUID.randomUUID()).make();
    }

    private Network newNetwork(String host) {
        Network net = mock(Network.class);
        when(net.getHostString()).thenReturn(host);
        return net;
    }

    private Multicaster<Listener, Message> synchronousMulticaster() {
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable task) {
                task.run();
            }
        };

        return new Multicaster<Listener, Message>(executor) {
            @Override
            protected void dispatch(Listener listener, Message message) {
                listener.onMessage(message);
            }
        };
    }

}
