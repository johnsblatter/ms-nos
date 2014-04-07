package com.workshare.msnos.spikes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.UUID;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.protocols.ip.MulticastSocketFactory;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;
import com.workshare.msnos.core.protocols.ip.udp.UDPServer;
import com.workshare.msnos.core.protocols.ip.udp.Utils;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.Multicaster;

public class TalkingSpike {
    
    private static final Cloud MYCLOUD = new Cloud(new UUID(123, 456));

    public static void main4(String[] args) throws Exception {
        File f = new File("tmp.txt");
        
        String json = "";
        BufferedReader in = new BufferedReader(new FileReader(f));
        try {
            json = in.readLine();
        } finally {
            in.close();
        }
        
        System.out.println(json);

        Message m = Json.fromJsonString(json, Message.class);
        System.out.println(m);
    }
    
    public static void main3(String[] args) throws Exception {
        File f = new File("tmp.bin");
        byte data[] = new byte[(int) f.length()];
        
        FileInputStream in = new FileInputStream(f);
        try {
            in.read(data);
        } finally {
            in.close();
        }
        
        final String json = new String(data, Charset.forName("UTF-8"));
        System.out.println(json);
        FileWriter out = new FileWriter("tmp.txt");
        try {
            out.write(json);
        }
        finally {
            out.close();
        }

        Message m = Json.fromJsonString(json, Message.class);
        System.out.println(m);
    }
    
    public static void main(String[] args) throws Exception {
        Agent me = new Agent(UUID.randomUUID()).join(MYCLOUD);
        System.out.printf("Hello, agent %s loaded!\n", me);
        
        Gateway gateway = new UDPGateway(sockets(), server(), caster(), me);
        gateway.addListener(new Listener(){
            @Override
            public void onMessage(Message message) {
                System.out.printf("- received message %s\n",message);
            }});
        System.out.printf("UDP gateway ready and listening!\n");

        for (int i = 0; i < 10; i++) {
            System.out.printf("Sleeping 5 secs...\n");
            Thread.sleep(5000);
            
            Message message = Utils.newSampleMessage().from(me.getIden()).to(MYCLOUD.getIden());
            gateway.send(message);
            System.out.printf("- sent message %s\n",message);
        }
        
        System.out.printf("Sleeping 60 secs...\n");
        Thread.sleep(20000);
        
        System.out.printf("Leaving now!\n");
        System.exit(0);
    }

    private static UDPServer server() {
        return new UDPServer();
    }

    private static MulticastSocketFactory sockets() {
        return new MulticastSocketFactory();
    }

    private static Multicaster<Listener, Message> caster() {
        return new Multicaster<Gateway.Listener, Message>() {
            @Override
            protected void dispatch(Gateway.Listener listener, Message message) {
                listener.onMessage(message);
            }
        };
    }
}
