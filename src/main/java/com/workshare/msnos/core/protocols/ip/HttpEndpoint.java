package com.workshare.msnos.core.protocols.ip;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Iden;

public class HttpEndpoint extends BaseEndpoint {

    private static final Logger log = LoggerFactory.getLogger(HttpEndpoint.class);
    private final String url;
    private final transient Iden target;

    public HttpEndpoint(Network host, String url) {
        this(host, url, Iden.NULL);
    }

    public HttpEndpoint(Network host, String url, Iden target) {
        super(Type.HTTP, host, extractPort(url));
        if (target == null)
            throw new IllegalArgumentException("Target cannot be null: pelase use Iden.NULL instead, thanks :)");
        
        this.url = url;
        this.target = target;
    }

    public String getUrl() {
        return url;
    }

    public Iden getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + url.hashCode();
        result = prime * result + target.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            HttpEndpoint other = (HttpEndpoint) obj;
            return super.equals(other) && url.equals(other.url) && target.equals(other.target);
        } catch (Exception ignore) {
            return false;
        }
    }

    private static short extractPort(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
            final int port = url.getPort();
            return (port == -1) ? Type.HTTP.defaultPort() : (short)port;
        } catch (MalformedURLException e) {
            log.warn("Malformed URL received: "+urlString, e);
            throw new IllegalArgumentException("Malformed URL: "+urlString);
        }
    }
}
