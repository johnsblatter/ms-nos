package com.workshare.msnos.soup.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.protocols.ip.HttpClientFactory;
import com.workshare.msnos.soup.threading.ExecutorServices;

public class DnsResolverWithTimeout implements DnsResolver {

    static final ExecutorService DNS_RESOLVE_EXECUTOR = ExecutorServices.newCachedDaemonThreadPool();

    private static final Logger log = LoggerFactory.getLogger(HttpClientFactory.class);

    private final ExecutorService executor;
    private final DnsResolver systemResolver;
    private final long timeoutInMillis;

    public DnsResolverWithTimeout() {
        this(DNS_RESOLVE_EXECUTOR, new SystemDefaultDnsResolver(), getDnsTimeout());
    }

    public DnsResolverWithTimeout(ExecutorService executor, DnsResolver systemResolver, long timeoutInMillis) {
        this.executor = executor;
        this.systemResolver = systemResolver;
        this.timeoutInMillis = timeoutInMillis;
        log.debug("DNS resolution with timeout of {} millis", timeoutInMillis);
    }

    private static int getDnsTimeout() {
        return Integer.getInteger("com.ws.msnos.dns.timeout", 5000);
    }

    @Override
    public InetAddress[] resolve(final String host) throws UnknownHostException {

        Future<InetAddress[]> result = executor.submit(new Callable<InetAddress[]>() {
            @Override
            public InetAddress[] call() throws Exception {
                return systemResolver.resolve(host);
            }
        });

        try {
            return result.get(timeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("Unexpected interrupton while resolving host "+host, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.warn("Unexpected execution exception", e.getCause());
        } catch (TimeoutException e) {
            log.warn("Timeout of {} millis elapsed resolving host {}", timeoutInMillis, host);
        }

        throw new UnknownHostException(host + ": DNS timeout");
    }
}
