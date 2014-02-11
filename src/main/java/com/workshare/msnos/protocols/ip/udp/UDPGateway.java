package com.workshare.msnos.protocols.ip.udp;

import java.io.IOException;
import java.util.Set;

import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.protocols.ip.Endpoint;

public class UDPGateway implements Gateway {

    private static final String DEFAULT_GROUP = System.getProperty("com.ws.nsnos.udp.group", "230.31.32.33");
    private static final int UDP_PORT = Integer.getInteger("com.ws.nsnos.udp.port", 2728);
    private static final int PORT_WIDTH = Integer.getInteger("com.ws.nsnos.udp.port.width", 3);
    private static final int PACKET_SIZE = Integer.getInteger("com.ws.nsnos.udp.packet.size", 512);

    @Override
    public void addListener(Listener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public Set<? extends Endpoint> endpoints() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean send(Message message) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

}
