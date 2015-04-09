package com.workshare.msnos.core;

import static com.workshare.msnos.core.CoreHelper.synchronousGatewayMulticaster;
import static org.mockito.Mockito.*;

import java.net.MulticastSocket;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.client.HttpClient;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.protocols.ip.http.HttpGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPServer;
import com.workshare.msnos.core.protocols.ip.www.HttpClientHelper;
import com.workshare.msnos.core.protocols.ip.www.WWWGateway;
import com.workshare.msnos.core.serializers.WireJsonSerializer;
import com.workshare.msnos.soup.threading.Multicaster;

public class GatewaysHelper {
    
    private static HttpClient httpClient;

    private GatewaysHelper() {

    }
    
    private static HttpClient httpClient() {
        try {
            if (httpClient == null)
                httpClient = new HttpClientHelper().client();
        } catch (Exception e) {
            throw new RuntimeException("wtf?");
        }
        
        return httpClient;
    }

    public static HttpGateway newHttpGateway() throws Exception {
        return new HttpGateway(httpClient());
    }

    public static UDPGateway newUDPGateway() throws Exception {
        Multicaster<Listener, Message> caster = synchronousGatewayMulticaster();
        MulticastSocketFactory sockets = mock(MulticastSocketFactory.class);
        MulticastSocket msock = mock(MulticastSocket.class);
        when(sockets.create()).thenReturn(msock);

        return new UDPGateway(sockets, mock(UDPServer.class), caster);
    }
    
    public static WWWGateway newWWWGateway() throws Exception {
        Multicaster<Listener, Message> caster = synchronousGatewayMulticaster();
        return new WWWGateway(httpClient(), mock(ScheduledExecutorService.class), new WireJsonSerializer(), caster);
    }
}
