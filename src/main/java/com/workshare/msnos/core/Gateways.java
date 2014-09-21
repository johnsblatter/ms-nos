package com.workshare.msnos.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.MsnosException.Code;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.HttpClientFactory;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPServer;
import com.workshare.msnos.core.protocols.ip.www.WWWGateway;
import com.workshare.msnos.core.serializers.WireJsonSerializer;
import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.soup.threading.Multicaster;

public class Gateways {

    private static Logger log = Logger.getLogger(Gateways.class);

    private static Set<Gateway> all;

	public synchronized static Set<Gateway> all() throws MsnosException {
		if (all == null) {
	        all = new LinkedHashSet<Gateway>();
			addGateway(buildUDPGateway());
            addGateway(buildWWWGateway());

            if (all.size() == 0)
                throw new MsnosException("Unable to create at least one gateway", Code.UNRECOVERABLE_FAILURE);
            
			addShutdownHook();
		}
		
		return all;
	}
	
	public synchronized static Set<Endpoint> endpoints() throws MsnosException {
        HashSet<Endpoint> points = new HashSet<Endpoint>();
	    for (Gateway gate : all()) {
            points.addAll(gate.endpoints().all());
        }
	    
	    return points;
	}

    private static void addGateway(final Gateway gateway) {
        if (gateway != null)
            all.add(gateway);
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
		    @Override
		    public void run() {
                log.info("Closing gateways...");
		        for (Gateway gate : all)
		            close(gate);
                log.info("done!");
		    }

            private void close(Gateway gate) {
                final String name = gate.getClass().getSimpleName();
                try {
                    log.info("- closing gateway "+name+"...");
                    gate.close();
                } catch (IOException ex) {
                    log.warn("Unexpected exception closing gateway "+name);
                }
            }
		});
    }

    private static UDPGateway buildUDPGateway() {
        try {
            final UDPServer server = new UDPServer();
            final MulticastSocketFactory ockets = new MulticastSocketFactory();
    		return new UDPGateway(ockets, server, newMulticaster());
        } catch (Throwable ex) {
            log.error("Unable to create UDP gateway", ex);
            return null;
        }
    }

    private static WWWGateway buildWWWGateway() {
        if (!System.getProperties().containsKey(WWWGateway.SYSP_ADDRESS)) {
            log.warn("Missing configuration for WWW gateway, please add property "+WWWGateway.SYSP_ADDRESS);
            return null;
        }
        
        try {
            return new WWWGateway(newHttpClient(), newScheduler(), new WireJsonSerializer(), newMulticaster()); 
        } catch (Throwable ex) {
            log.error("Unable to create WWW gateway", ex);
            return null;
        }
    }

    private static HttpClient newHttpClient() {
        return HttpClientFactory.newHttpClient();
    }

    private static ScheduledExecutorService newScheduler() {
        return ExecutorServices.newSingleThreadScheduledExecutor();
    }

    private static Multicaster<Listener, Message> newMulticaster() {
        final Multicaster<Listener, Message> caster = new Multicaster<Gateway.Listener, Message>() {
			@Override
			protected void dispatch(Gateway.Listener listener, Message message) {
				listener.onMessage(message);
			}
		};
        return caster;
    }
}
