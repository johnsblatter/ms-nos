package com.workshare.msnos.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.MsnosException.Code;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPServer;
import com.workshare.msnos.core.protocols.ip.www.WWWGateway;
import com.workshare.msnos.core.serializers.WireJsonSerializer;
import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.soup.threading.Multicaster;

public class Gateways {

    private static Logger log = Logger.getLogger(Gateways.class);

    private static HashSet<Gateway> all;

	public synchronized static Set<Gateway> all() throws MsnosException {
		if (all == null) {
	        all = new HashSet<Gateway>();
			addGateway(buildUDPGateway());
            addGateway(buildWWWGateway());

            if (all.size() == 0)
                throw new MsnosException("Unable to create at least one gateway", Code.UNRECOVERABLE_FAILURE);
            
			addShutdownHook();
		}
		
		return all;
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
    
    private static HttpClient newHttpClient() {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(getHttpMaxTotalConnections());
        cm.setDefaultMaxPerRoute(getHttpMaxDefaultConnectionsPerRoute());
        
        final RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(getHttpConnectTimeout())
                .setConnectTimeout(getHttpSocketTimeout())
                .build();
        
        final CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .setConnectionManager(cm)
                .setUserAgent(getHttpUserAgent())
                .build();     

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                try {
                    httpClient.close();
                } catch (IOException ex) {
                    log.debug("Error closing HTTP client", ex);
                }
            }
        });
        return httpClient;
    }

    private static String getHttpUserAgent() {
        return System.getProperty("com.ws.nsnos.http.user-agent", "com.msnos.client-v1.0");
    }

    private static int getHttpSocketTimeout() {
        return Integer.getInteger("com.ws.nsnos.http.timeout.socket", 10000);
    }

    private static int getHttpConnectTimeout() {
        return Integer.getInteger("com.ws.nsnos.http.timeout.connection", 10000);
    }

    private static int getHttpMaxTotalConnections() {
        return Integer.getInteger("com.ws.nsnos.http.max.total.connection.num", 200);
    }
    
    private static int getHttpMaxDefaultConnectionsPerRoute() {
        return Integer.getInteger("com.ws.nsnos.http.max.route.connection.num", 200);
    }

}
