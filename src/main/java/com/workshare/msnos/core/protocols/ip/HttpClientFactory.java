package com.workshare.msnos.core.protocols.ip;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.soup.ShutdownHooks;
import com.workshare.msnos.soup.ShutdownHooks.Hook;
import com.workshare.msnos.soup.net.DnsResolverWithTimeout;

public class HttpClientFactory {
    
    private static final Logger log = LoggerFactory.getLogger(HttpClientFactory.class);

    private static HttpClient shared;
  
    /**
     * The singleton is built this way because of the crappy implementation
     * of the Powemock cglib :( [bb]
     * (I knew I should not have used it)
     * @see MicroserviceLocationTest
     */
    public synchronized static HttpClient sharedHttpClient() {
        if (shared == null)
            shared = newHttpClient();
        
        return shared;
    }
    
    public static HttpClient newHttpClient() {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(getDefaultRegistry(), getDnsResolver());
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

        ShutdownHooks.addHook(new Hook(){
            @Override
            public void run() {
                try {
                    log.info("Shutting down HTTP client...");
                    httpClient.close();
                } catch (IOException ex) {
                    log.debug("Error closing HTTP client", ex);
                } finally {
                    log.info("done!");
                }
            }

            @Override
            public String name() {
                return "HTTP client closing";
            }

            @Override
            public int priority() {
                return Integer.MIN_VALUE;
            }});
        
        return httpClient;
    }

    public static String getHttpUserAgent() {
        return System.getProperty("com.ws.msnos.http.user-agent", "com.msnos.client-v1.0");
    }

    public static int getHttpSocketTimeout() {
        return Integer.getInteger("com.ws.msnos.http.timeout.socket", 10000);
    }

    public static int getHttpConnectTimeout() {
        return Integer.getInteger("com.ws.msnos.http.timeout.connection", 10000);
    }

    public static int getHttpMaxTotalConnections() {
        return Integer.getInteger("com.ws.msnos.http.max.total.connection.num", 200);
    }
    
    public static int getHttpMaxDefaultConnectionsPerRoute() {
        return Integer.getInteger("com.ws.msnos.http.max.route.connection.num", 200);
    }

    private static Registry<ConnectionSocketFactory> getDefaultRegistry() {
        return RegistryBuilder.<ConnectionSocketFactory>create()
            .register("https", SSLConnectionSocketFactory.getSocketFactory())
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .build();
    }

    private static DnsResolver getDnsResolver() {
        return new DnsResolverWithTimeout();
    }
}
