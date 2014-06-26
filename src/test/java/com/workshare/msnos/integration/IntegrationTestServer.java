package com.workshare.msnos.integration;

import java.io.IOException;
import java.util.UUID;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Cloud.Listener;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;

public class IntegrationTestServer implements IntegrationTest {

    private LocalAgent agent;
    private Cloud cloud;

    public IntegrationTestServer() throws IOException {
        agent = new LocalAgent(UUID.randomUUID());
        cloud = new Cloud(CLOUD_UUID);
        cloud.addListener(new Listener() {
            @Override
            public void onMessage(Message message) {
                if (message.getType() == Message.Type.APP)
                    process(message);
            }});

        agent.join(cloud);

    }

    private void process(Message message) {
        System.err.println("Received message: "+message);
        
    }

    private void run() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            System.err.printf("Intergation test server is alive :) step %d out of 10\n", i+1);
            Thread.sleep(5000L);
        }
        
        System.err.println("Goodbye!");
    }
    
    public static void main(String[] args) throws Exception {
        new IntegrationTestServer().run();
    }
}
