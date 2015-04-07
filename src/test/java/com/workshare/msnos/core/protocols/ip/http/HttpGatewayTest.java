package com.workshare.msnos.core.protocols.ip.http;

import static com.workshare.msnos.core.Iden.Type.AGT;
import static com.workshare.msnos.core.Iden.Type.CLD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.CoreHelper;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.protocols.ip.BaseEndpoint;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoints;
import com.workshare.msnos.core.protocols.ip.HttpEndpoint;
import com.workshare.msnos.core.protocols.ip.www.HttpClientHelper;
import com.workshare.msnos.core.serializers.WireJsonSerializer;

public class HttpGatewayTest {

    private static final Iden AGENT_SMITH = newIden(AGT);
    private static final String AGENT_SMITH_URL = "http://agents.smith:123/foo";

    private static final String SAMPLE_HOST = "21.21.21.21";
    private static final HttpEndpoint SAMPLE_ENDPOINT = new HttpEndpoint(CoreHelper.asPublicNetwork(SAMPLE_HOST), "http://123.com", newIden(AGT));

    private HttpGateway gate;
    private Cloud cloud;
    private HttpClientHelper http;
    private WireJsonSerializer sz;
    
    @Before
    public void setup() throws Exception {
        
        http = new HttpClientHelper();
        gate = new HttpGateway(http.client());

        cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(newIden(CLD));
        when(cloud.getRing()).thenReturn(Ring.random());
        
        sz = new WireJsonSerializer();

        installEndpoint("25.25.25.25", AGENT_SMITH, AGENT_SMITH_URL);
    }

    @Test
    public void shouldReturnInstallableEndpoints() throws Exception {

        Endpoints endpoints = gate.endpoints();
        Endpoint ep1 = endpoints.install(SAMPLE_ENDPOINT);
        
        assertTrue(endpoints.all().contains(ep1));
    }
    
    @Test
    public void shouldReturnRemovableEndpoints() throws Exception {

        Endpoints endpoints = gate.endpoints();
        int currentSize = endpoints.all().size();

        Endpoint ep1 = endpoints.install(SAMPLE_ENDPOINT);
        endpoints.remove(ep1);
        
        assertEquals(currentSize, endpoints.all().size());
    }

    @Test(expected=MsnosException.class)
    public void shouldRefuseToInstallNonHttpEndpoints() throws Exception {
        gate.endpoints().install(new BaseEndpoint(Endpoint.Type.UDP, CoreHelper.asPublicNetwork("10.10.10.1")));
    }
    
    @Test
    public void shouldStampReceiptWithGatewayName() throws Exception {
        Message message = newSampleMessage(newIden(AGT), newIden(CLD));
        Receipt receipt = gate.send(cloud, message);

        assertEquals("HTTP", receipt.getGate());
    }

    // FIXME the test about sending the message to all agents outside my ring it's missing :(
    @Test
    public void shouldReturnFailedDeliveryReceiptWhenMessageDirectedToCloudAndNoMeansToDeliverIt() throws Exception {
        
        Message message = newSampleMessage(newIden(AGT), newIden(CLD));
        Receipt receipt = gate.send(cloud, message);

        assertEquals(Message.Status.FAILED, receipt.getStatus());
    }

    @Test
    public void shouldReturnFailedDeliveryReceiptWhenNoRouteToTarget() throws Exception {
       
        Message message = newSampleMessage(newIden(AGT), newIden(AGT));
        Receipt receipt = gate.send(cloud, message);

        assertEquals(Message.Status.FAILED, receipt.getStatus());
    }

    @Test
    public void shouldSendTheMessageUsingHttpClient() throws Exception {

        Message message = newSampleMessage(newIden(AGT), AGENT_SMITH);
        Receipt receipt = gate.send(cloud, message);

        HttpPost request = http.getLastPostToWWW();
        assertNotNull(request);
        assertEquals(AGENT_SMITH_URL, request.getURI().toString());
        assertEquals(toText(message), toText(request.getEntity()));
        assertEquals(Message.Status.DELIVERED, receipt.getStatus());
    }

    @Test
    public void shouldHandleFailures() throws Exception {

        when(http.client().execute(any(HttpUriRequest.class))).thenThrow(new IOException("boom!"));

        Receipt receipt = gate.send(cloud, newSampleMessage(newIden(AGT), AGENT_SMITH));

        assertEquals(Message.Status.FAILED, receipt.getStatus());
    }


    private String toText(Message message) {
        return sz.toText(message);
    }

    public String toText(HttpEntity entity) throws ParseException, IOException {
        return EntityUtils.toString(entity);
    }

    private void installEndpoint(final String host, Iden agent, final String url) throws MsnosException {
        gate.endpoints().install(new HttpEndpoint(CoreHelper.asPublicNetwork(host), url, agent));
    }
    
    private static Iden newIden(final com.workshare.msnos.core.Iden.Type idenType) {
        return new Iden(idenType, UUID.randomUUID());
    }
    
    private Message newSampleMessage(Iden from, Iden to) {
        return new MessageBuilder(Type.APP, from, to).with(UUID.randomUUID()).make();
    }
}
