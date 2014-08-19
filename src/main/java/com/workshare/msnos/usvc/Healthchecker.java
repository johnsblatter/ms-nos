package com.workshare.msnos.usvc;

import com.workshare.msnos.usvc.api.RestApi;
import com.workshare.msnos.usvc.api.RestApi.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Healthchecker {

    private static final Logger log = LoggerFactory.getLogger(Healthchecker.class);
    private static final long CHECK_PERIOD = Long.getLong("msnos.usvc.health.check.time", 60000L);

    private final Microservice microservice;
    private final ScheduledExecutorService scheduler;

    public Healthchecker(Microservice microservice, ScheduledExecutorService executorService) {
        this.microservice = microservice;
        scheduler = executorService;
    }

    public void run() {
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                healthCheckApis();
            }
        }, CHECK_PERIOD, CHECK_PERIOD, TimeUnit.MILLISECONDS);
    }

    private void healthCheckApis() {
        for (RemoteMicroservice remote : microservice.getMicroServices()) {
            for (RestApi rest : remote.getApis()) {
                if (rest.getType() == Type.HEALTHCHECK) {
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) new URL(rest.getUrl()).openConnection();
                        connection.setRequestMethod("HEAD");
                        int responseCode = connection.getResponseCode();
                        if (responseCode != 200) {
                            handleIllMicroservice(remote);
                        } else {
                            handleWellMicroservice(remote);
                        }
                    } catch (IOException e) {
                        log.error("Unable to health check restApi URL for " + remote);
                        handleIllMicroservice(remote);
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }
            }
        }
    }

    private void handleIllMicroservice(RemoteMicroservice remote) {
        for (RestApi rest : remote.getApis()) {
            rest.markFaulty();
        }
    }

    private void handleWellMicroservice(RemoteMicroservice remote) {
        for (RestApi rest : remote.getApis()) {
            rest.markWorking();
        }
    }
}
