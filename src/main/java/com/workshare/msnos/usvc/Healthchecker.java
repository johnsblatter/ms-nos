package com.workshare.msnos.usvc;

import static com.workshare.msnos.core.Message.Type.ENQ;
import static com.workshare.msnos.core.Message.Type.HCK;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.payloads.HealthcheckPayload;
import com.workshare.msnos.core.protocols.ip.HttpClientFactory;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.RestApi.Type;

public class Healthchecker {
    private static final Logger log = LoggerFactory.getLogger(Healthchecker.class);

    public static final String SYSP_CHECK_PERIOD = "msnos.usvc.health.check.time";
    public static final String SYSP_ENQ_PERIOD = "msnos.usvc.health.enq.period";

    public static final long CHECK_PERIOD = Long.getLong(SYSP_CHECK_PERIOD, 60000L);
    public static final long ENQ_PERIOD = Long.getLong(SYSP_ENQ_PERIOD, 3 * CHECK_PERIOD);
    public static final int TIMEOUT_CONN = HttpClientFactory.getHttpConnectTimeout();
    public static final int TIMEOUT_READ = HttpClientFactory.getHttpSocketTimeout();

    private final Microcloud microcloud;
    private final ScheduledExecutorService scheduler;

    public Healthchecker(Microcloud microcloud, ScheduledExecutorService executorService) {
        this.microcloud = microcloud;
        this.scheduler = executorService;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                healthCheckApis();
                enquiryApis();
            }
        }, CHECK_PERIOD, CHECK_PERIOD, TimeUnit.MILLISECONDS);
    }

    private void healthCheckApis() {
        for (RemoteMicroservice remote : microcloud.getMicroServices()) {
            final long elapsed = SystemTime.asMillis() - remote.getLastChecked();
            if (elapsed > 0 && elapsed < CHECK_PERIOD) {
                log.debug("Skipping check for microservice {}: recently reported checked", remote);
                continue;
            }

            Boolean faulty = null;
            for (RestApi rest : remote.getApis()) {
                if (rest.getType() == Type.HEALTHCHECK) {
                    if (isReportingHealthy(remote, rest)) {
                        faulty = Boolean.FALSE;
                        break;
                    } else {
                        faulty = Boolean.TRUE;
                    }
                }
            }

            reporServiceStatus(remote, faulty);
        }
    }

    private void reporServiceStatus(RemoteMicroservice remote, Boolean faulty) {
        if (faulty == null) {
            log.debug("Cannot healtheck microservice {}: no endpoints", remote);
            return;
        }

        if (faulty == Boolean.TRUE) {
            log.info("Remote microservice {} found faulty", remote);
            remote.markFaulty();
        } else {
            log.debug("Remote microservice {} found working", remote);
            remote.markWorking();
        }

        try {
            microcloud.send(newHealthMessage(remote, !faulty));
        } catch (MsnosException e) {
            log.warn("Unable to send health status message to the cloud", e);
        }
    }

    private boolean isReportingHealthy(RemoteMicroservice remote, RestApi rest) {
        try {
            HttpURLConnection connection = null;
            try {
                final URL url = new URL(rest.getUrl());
                log.debug("Healtcheck running agains url {}...", url);
               
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(TIMEOUT_CONN);
                connection.setReadTimeout(TIMEOUT_READ);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                final boolean working = responseCode == 200;
                if (working) {
                    log.debug("Remote microservice {} found working on healthcheck {}", remote);
                    return true;
                } else {
                    log.debug("Remote microservice {} found faulty on healthcheck {} with status {}", remote, responseCode);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        } catch (Throwable ex) {
            log.warn("Unrecoverable exception while accessing remote microservice {} at URL {}: {}", remote.getName(), rest.getUrl(), toString(ex));
        }
        
        log.debug("Remote microservice {} found faulty on healthcheck {} because of I/O issues", remote);
        return false;
    }

    private String toString(Throwable ex) {
        return ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }


    private void enquiryApis() {
        long now = SystemTime.asMillis();
        for (RemoteMicroservice remote : microcloud.getMicroServices()) {
            if (remote.isFaulty())
                continue;

            if ((now - remote.getLastUpdated()) < ENQ_PERIOD)
                continue;

            try {
                Message message = new MessageBuilder(ENQ, microcloud.getCloud(), remote.getAgent()).make();
                microcloud.send(message);
            } catch (Exception ex) {
                log.error("Unable to send an ENQ to remote agent " + remote.getAgent(), ex);
            }
        }
    }

    private Message newHealthMessage(RemoteMicroservice remote, boolean working) {
        return new MessageBuilder(HCK, microcloud.getCloud(), microcloud.getCloud()).with(new HealthcheckPayload(remote.getAgent(), working)).make();
    }

}
