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
import com.workshare.msnos.core.payloads.HealthcheckPayload;
import com.workshare.msnos.soup.time.SystemTime;
import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.RestApi.Type;

public class Healthchecker {
    private static final Logger log = LoggerFactory.getLogger(Healthchecker.class);

    public static final String SYSP_CHECK_PERIOD = "msnos.usvc.health.check.time";
    public static final String SYSP_ENQ_PERIOD = "msnos.usvc.health.enq.period";
    public static final String SYSP_TIMEOUT_CONN = "msnos.usvc.health.connection.timeout.open";
    public static final String SYSP_TIMEOUT_READ = "msnos.usvc.health.connection.timeout.read";
    
    public static final long CHECK_PERIOD = Long.getLong(SYSP_CHECK_PERIOD, 60000L);
    public static final long ENQ_PERIOD = Long.getLong(SYSP_ENQ_PERIOD, 5*CHECK_PERIOD);
    public static final int TIMEOUT_CONN = Integer.getInteger(SYSP_TIMEOUT_CONN, 30000);
    public static final int TIMEOUT_READ = Integer.getInteger(SYSP_TIMEOUT_READ, 120000);

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
                log.debug("Skipping check for microservice {}: recently reported checked",remote);
                continue;
            }
            
            for (RestApi rest : remote.getApis()) {
                if (rest.getType() == Type.HEALTHCHECK) {
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) new URL(rest.getUrl()).openConnection();
                        connection.setConnectTimeout(TIMEOUT_CONN);
                        connection.setReadTimeout(TIMEOUT_READ);
                        connection.setRequestMethod("HEAD");
                        int responseCode = connection.getResponseCode();
                        final boolean working = responseCode == 200;
                        if (working) {
                            log.debug("Remote microservice {} found working",remote);
                            remote.markWorking();
                        } else {
                            log.info("Remote microservice {} found faulty: {}",remote, responseCode);
                            remote.markFaulty();
                        }
                        microcloud.send(newHealthMessage(remote, working));
                    } catch (Exception ex) {
                        log.error("Unable to health check restApi URL for " + remote, ex);
                        remote.markFaulty();
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }
            }
        }
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
            }
            catch (Exception ex) {
                log.error("Unable to send an ENQ to remote agent "+remote.getAgent(), ex);
            }
        }
    }

    private Message newHealthMessage(RemoteMicroservice remote, boolean working) {
        return new MessageBuilder(HCK, microcloud.getCloud(), microcloud.getCloud())
                .with(new HealthcheckPayload(remote.getAgent(), working))
                .make();
    }

}
