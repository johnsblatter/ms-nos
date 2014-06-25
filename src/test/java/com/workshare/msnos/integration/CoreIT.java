package com.workshare.msnos.integration;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.LocalAgent;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

public class CoreIT {

    private LocalAgent agent;
    private Cloud cloud;

    @Before
    public void setUp() throws Exception {
        agent = new LocalAgent(UUID.randomUUID());
        cloud = new Cloud(new UUID(111, 222));
        agent.join(cloud);
    }

    @Test
    public void shouldSeePresenceFromRemoteOnJoin() throws Exception {
        Process p = createProcess("java", "-jar", "~/Documents/Java/ms-nos-core-client/target/ms-nos-core-client-0.0.1-SNAPSHOT.jar");
        int exitValue = printExitValue(p);
    }

    private Process createProcess(String java, String s, String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(java, s, command);
        Process process = pb.start();
        redirentProcessIO(process);
        return process;
    }

    private void redirentProcessIO(Process p) throws IOException {
        InputStream is = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
    }

    private int printExitValue(Process p) {
        int exitValue = 0;
        try {
            exitValue = p.waitFor();
            System.out.println("\n\nExit Value is " + exitValue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return exitValue;
    }
}
