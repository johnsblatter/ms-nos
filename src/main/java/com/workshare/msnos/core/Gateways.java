package com.workshare.msnos.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPServer;
import com.workshare.msnos.soup.threading.Multicaster;

public class Gateways {

	private static HashSet<Gateway> all;

	public synchronized static Set<Gateway> all() throws MsnosException {
		if (all == null) {
			all = new HashSet<Gateway>();
			addUDPGateway();
		}
		
		return all;
	}

    private static void addUDPGateway() throws MsnosException {
        try {
            all.add(buildUDPGateway());
        } catch (IOException ex) {
            throw new MsnosException("Unable to build UDP gatway", ex);
        }
    }

	private static UDPGateway buildUDPGateway() throws IOException {
		final UDPServer server = new UDPServer();
		final MulticastSocketFactory ockets = new MulticastSocketFactory();
		final Multicaster<Listener, Message> caster = new Multicaster<Gateway.Listener, Message>() {
			@Override
			protected void dispatch(Gateway.Listener listener, Message message) {
				listener.onMessage(message);
			}
		};

		return new UDPGateway(ockets, server, caster);
	}
}
