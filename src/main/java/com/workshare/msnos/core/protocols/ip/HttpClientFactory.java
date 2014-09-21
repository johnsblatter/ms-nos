package com.workshare.msnos.core.protocols.ip;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientFactory {
    
    private static final Logger log = LoggerFactory.getLogger(HttpClientFactory.class);

    private static HttpClient shared = newHttpClient();
    
    public static HttpClient sharedHttpCliet() {
        return shared;
    }
    
    public static HttpClient newHttpClient() {
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
