package com.workshare.msnos.integration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.UUID;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Cloud.Listener;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.payloads.GenericPayload;
import com.workshare.msnos.soup.json.Json;

public class SupportServer implements IntegrationTestFrame {

    protected LocalAgent masterAgent;
    protected Cloud masterCloud;

    protected LocalAgent testAgent;
    protected Cloud testCloud;

    public SupportServer() throws IOException {
        bootstrap();

        masterCloud.addListener(new Listener() {
            @Override
            public void onMessage(Message message) {
                if (message.getType() == Message.Type.APP) {
                    final Payload payload = message.getData();
                    if (payload instanceof GenericPayload)
                        try {
                            processCommand(CommandPayload.create((GenericPayload) payload));
                        } catch (MsnosException e) {
                            log("Unexpected exception while processing a command from the test client! "+e.getMessage());
                        }
                }
            }
        });

    }

    public void bootstrap() throws MsnosException {
        masterAgent = new LocalAgent(UUID.randomUUID());
        masterCloud = new Cloud(MASTER_CLOUD_UUID);
        masterAgent.join(masterCloud);

        testCloud = new Cloud(GENERIC_CLOUD_UUID);
        testAgent = new LocalAgent(UUID.randomUUID());
    }

    protected void processCommand(CommandPayload payload) throws MsnosException {
        log("Received message: " + Json.toJsonString(payload));
        switch (payload.getCommand()) {
            case AGENT_JOIN:
                log("Agent joining the cloud");
                testAgent.join(testCloud);
                break;

            case AGENT_LEAVE:
                log("Agent leaving the cloud...");
                testAgent.leave();
                break;

            case SELF_KILL:
                log("Suiciding ;(");
                System.exit(-1);
                break;
        }

    }

    private void run() throws InterruptedException {
        final int total = 10;
        for (int i = 0; i < total; i++) {
            log("Intergation test server is alive :) step "+(i+1)+" out of "+total);
            Thread.sleep(5000L);
        }

        log("Goodbye!");
    }

    public static void main(String[] args) throws Exception {
        boolean okay = false;
        
        if (args.length == 1) {
            if ("now".equalsIgnoreCase(args[0])) {
                okay = true;
                runNow();
            }
            else if ("fork".equalsIgnoreCase(args[0])) {
                okay = true;
                runFork();
            }
        }

        if (!okay) {
            log("Invalid parameters specified: " + Arrays.asList(args));
            log("Please use either \"fork\" or \"now\"");
        }
    }

    private static void runNow() throws Exception {
        log("Running in process");
        new SupportServer().run();
    }

    private static void runFork() throws Exception {
        log("Running forked");
        String javacmd = new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();

        String[] args = new String[] { javacmd, "-cp", System.getProperty("java.class.path"),SupportServer.class.getCanonicalName(), "now" };
        log("args: "+Arrays.asList(args));
        Runtime.getRuntime().exec(args);
    }

    private static void log(String what) {
        try {
            PrintWriter log = new PrintWriter(new FileWriter(new File("/tmp/tmp.log"), true));
            try {
                log.println(what);
            } finally {
                log.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
