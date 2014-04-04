package com.workshare.msnos.spikes;

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
import com.workshare.msnos.soup.threading.Multicaster;

public class TalkingSpike {
    
    private static final Cloud MYCLOUD = new Cloud(new UUID(123, 456));
    
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
            Message message = Utils.newSampleMessage().from(me.getIden()).to(MYCLOUD.getIden());
            gateway.send(message);
            System.out.printf("- sent message %s\n",message);
            Thread.sleep(5000);
        }
        
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
