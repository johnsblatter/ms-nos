package com.workshare.msnos.core.protocols.ip;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.protocols.ip.resolvers.CompositeIPResolver;
import com.workshare.msnos.core.protocols.ip.resolvers.IPResolver;
import com.workshare.msnos.core.protocols.ip.resolvers.IPResolverBySystemProperty;
import com.workshare.msnos.core.protocols.ip.resolvers.IPResolverByURL;

public class AddressResolver {

    public static final String SYSP_PUBLIC_IP = "com.ws.msnos.address.public.ip";
    public static final String SYSP_ROUTER_IP = "com.ws.msnos.address.router.ip";

    // instance-data is usually 169.254.169.254
    public static final String AMAZON_IPV4_DISCOVERY_ENDPOINT = "http://instance-data/latest/meta-data/public-ipv4";
    public static final String ROUTER_DISCOVERY_ENDPOINT = "http://checkip.amazonaws.com";

    public static IPResolver FOR_ROUTER_IP = new CompositeIPResolver(
            new IPResolverBySystemProperty(SYSP_ROUTER_IP),
            new IPResolverByURL(ROUTER_DISCOVERY_ENDPOINT)
        );

    public static IPResolver FOR_PUBLIC_IP = new CompositeIPResolver(
            new IPResolverBySystemProperty(SYSP_PUBLIC_IP),
            new IPResolverByURL(AMAZON_IPV4_DISCOVERY_ENDPOINT)
        );

    private static Logger log = LoggerFactory.getLogger(AddressResolver.class);

    private final HttpClient httpClient;
    private final IPResolver routerIpResolver;
    private final IPResolver publicIpResolver;

    public AddressResolver() {
        this(HttpClientFactory.sharedHttpClient(), FOR_ROUTER_IP, getPublicResolver());
    }

    private static IPResolver getPublicResolver() {
        if ("router".equalsIgnoreCase(System.getProperty(SYSP_PUBLIC_IP))) {
            log.info("Using router resolver for public IP as requested");
            return FOR_ROUTER_IP;
        }
        else
            return FOR_PUBLIC_IP;
    }

    public AddressResolver(HttpClient httpClient, IPResolver routerIpResolver, IPResolver publicIpResolver) {
        this.httpClient = httpClient;
        this.routerIpResolver = routerIpResolver;
        this.publicIpResolver = publicIpResolver;
    }

    public HttpClient httpClient() {
        return httpClient;
    }
    
    public Network findRouterIP() {
        return findIP(routerIpResolver, "external");
    }

    public Network findPublicIP() {
        return findIP(publicIpResolver, "public");
    }

    private Network findIP(final IPResolver resolver, String type) {
        Network result = newNetwork(resolver.resolve(this));
        if (result == null)
            log.debug("Unable to collect {} IP", type);
        else
            log.debug("Using {} address {}", type, result);

        return result;
    }
    
    private Network newNetwork(byte[] ip) {
        return (ip != null) ? new Network(ip, (short) 32) : null;
    }
    
    public byte[] getIPViaURL(final String url) {
        byte[] address = null;
        try {
            HttpEntity entity = null;
            try {
                final HttpGet request = new HttpGet(url);
                log.debug("Getting public address trough {}", request.getURI());
                HttpResponse response = httpClient.execute(request);
                if (response != null) {
                    entity = response.getEntity();
                    final String text = EntityUtils.toString(entity);
                    log.debug("Get result: {}", text);
                    address = (entity == null) ? null : Network.createAddressFromString(text);
                    log.debug("Got address: {}", address);
                }
            } finally {
                EntityUtils.consume(entity);
            }
        } catch (Throwable ex) {
            log.debug("Unable to resolve IP from url "+url, ex);
        }

        return address;
    }

    protected IPResolver getRouterIpResolver() {
        return routerIpResolver;
    }

    protected IPResolver getPublicIpResolver() {
        return publicIpResolver;
    }
}
