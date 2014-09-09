package com.workshare.msnos.core.protocols.ip;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.InetAddress;

public class AddressResolver {

    private final HttpClient httpClient;

    public AddressResolver() {
        httpClient = HttpClients.createDefault();
    }

    public AddressResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Network findPublicIP() throws IOException {
        Network result;
        if (System.getProperty("public.ip") != null) {
            result = new Network(createAddressFromString(System.getProperty("public.ip")), (short) 32);
        } else {
            result = new Network(getPublicFromAWS(), (short) 32);
        }

        return result;
    }

    private byte[] getPublicFromAWS() throws IOException {
        HttpEntity entity = null;
        try {
            HttpResponse response = httpClient.execute(new HttpGet("http://instance-data/latest/meta-data/public-ipv4"));
            if (response != null) {
                entity = response.getEntity();
            }
        } finally {
            EntityUtils.consume(entity);
        }
        return createAddressFromString(EntityUtils.toString(entity));
    }

    private byte[] createAddressFromString(String address) throws IOException {
        return InetAddress.getByName(address).getAddress();
    }
}
