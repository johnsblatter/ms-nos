package com.workshare.msnos.core.protocols.ip;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressResolver {

    // instance-data is usually 169.254.169.254
    private static final String AMAZON_IPV4_DISCOVERY_ENDPOINT = "http://instance-data/latest/meta-data/public-ipv4";

    private static Logger log = LoggerFactory.getLogger(AddressResolver.class);

    private final HttpClient httpClient;

    public AddressResolver() {
        httpClient = HttpClientFactory.sharedHttpCliet();
    }

    public AddressResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Network findPublicIP() throws IOException {
        Network result = null;
        if (System.getProperty("public.ip") != null) {
            result = new Network(createAddressFromString(System.getProperty("public.ip")), (short) 32);
        } else {
            byte[] publicFromAWS = getPublicFromAWS();
            if (publicFromAWS != null)
                result = new Network(publicFromAWS, (short) 32);
        }

        if (result == null)
            log.info("Unable to collect public IP");
        else
            log.info("Using public address {}", result);

        return result;
    }

    private byte[] getPublicFromAWS() {
        byte[] address = null;
        try {
            HttpEntity entity = null;
            try {
                final HttpGet request = new HttpGet(AMAZON_IPV4_DISCOVERY_ENDPOINT);
                log.debug("Getting public address trough {}", request.getURI());
                HttpResponse response = httpClient.execute(request);
                if (response != null) {
                    entity = response.getEntity();
                    address = (entity == null) ? null : createAddressFromString(EntityUtils.toString(entity));
                }
            } finally {
                EntityUtils.consume(entity);
            }
        } catch (Throwable ex) {
            log.debug("Unable to resolve IP from AWS", ex);
        }

        return address;
    }

    private byte[] createAddressFromString(String address) throws IOException {
        return InetAddress.getByName(address).getAddress();
    }
}
