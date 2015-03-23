package com.workshare.msnos.core.protocols.ip;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressResolver {

    public static final String SYSP_PUBLIC_IP = "com.ws.msnos.public.ip";
    public static final String SYSP_EXTERNAL_IP = "com.ws.msnos.external.ip";

    // instance-data is usually 169.254.169.254
    public static final String AMAZON_IPV4_DISCOVERY_ENDPOINT = "http://instance-data/latest/meta-data/public-ipv4";
    public static final String AMAZON_EXTERNAL_DISCOVERY_ENDPOINT = "http://checkip.amazonaws.com";
    
    private static Logger log = LoggerFactory.getLogger(AddressResolver.class);

    private final HttpClient httpClient;

    public AddressResolver() {
        httpClient = HttpClientFactory.sharedHttpClient();
    }

    public AddressResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Network findExternalIP() throws IOException {
        Network result = null;
        if (System.getProperty(SYSP_EXTERNAL_IP) != null) {
            result = new Network(createAddressFromString(System.getProperty(SYSP_EXTERNAL_IP)), (short) 32);
            log.debug("external ip loaded from system property: {}", result);
        } else {
            result = newNetwork(getIPFromAWS(AMAZON_EXTERNAL_DISCOVERY_ENDPOINT));
            log.debug("public ip loaded from amazon: {}", result);
        }

        if (result == null)
            log.info("Unable to collect public IP");
        else
            log.info("Using public address {}", result);

        return result;
    }
    
    public Network findPublicIP() throws IOException {
        Network result = null;
        if (System.getProperty(SYSP_PUBLIC_IP) != null) {
            result = new Network(createAddressFromString(System.getProperty(SYSP_PUBLIC_IP)), (short) 32);
            log.debug("public ip loaded from system property: {}", result);
        } else {
            result = newNetwork(getIPFromAWS(AMAZON_IPV4_DISCOVERY_ENDPOINT));
            log.debug("public ip loaded from amazon: {}", result);
        }

        if (result == null)
            log.info("Unable to collect public IP");
        else
            log.info("Using public address {}", result);

        return result;
    }

    private Network newNetwork(byte[] ip) {
        return (ip != null) ? new Network(ip, (short) 32) : null;
    }
    
    private byte[] getIPFromAWS(final String url) {
        byte[] address = null;
        try {
            HttpEntity entity = null;
            try {
                final HttpGet request = new HttpGet(url);
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
        try {
            return InetAddress.getByName(address).getAddress();
        } catch(UnknownHostException ex) {
            log.debug("Failed to resolve host {} (DNS problem?) let's check if it's a x.y.z.k address", address);
            if (Network.isValidDottedIpv4Address(address)) {
                String[] nibbles = address.trim().split("\\.");
                byte[] bytes = new byte[nibbles.length];
                for (int i=0; i<nibbles.length; i++) {
                    int ival = Integer.valueOf(nibbles[i]);
                    bytes[i] = (byte)(ival&0xff);
                }

                if (log.isDebugEnabled())
                    log.debug("Address resolved to {}", Arrays.asList(bytes));

                return bytes;
            } else {
                log.debug("Address {} NOT resolved :(", address);
                return null;
            }
            
        }
    }
}
