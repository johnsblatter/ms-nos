package com.workshare.msnos.core;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.threading.Multicaster;
import com.workshare.msnos.soup.time.SystemTime;

public class CoreHelper {

    private CoreHelper() {}
    
    public static UUID randomUUID() {
        return UUID.randomUUID();
    }

    public static Iden newCloudIden() {
        return new Iden(Iden.Type.CLD, UUID.randomUUID());
    }

    public static Iden newAgentIden() {
        return new Iden(Iden.Type.AGT, UUID.randomUUID());
    }

    public static <T> Set<T> asSet(T... items) {
        return new HashSet<T>(Arrays.asList(items));
    }

    public static Network asPublicNetwork(String host) {
        return asNetwork(host, (short)1);
    }

    public static Network asNetwork(String host, short prefix) {
        return new Network(toByteArray(host), prefix);
    }

    public static byte[] toByteArray(String host) {
        String[] tokens = host.split("\\.");
        byte[] addr = new byte[4];
        for (int i = 0; i < addr.length; i++) {
            addr[i] = Byte.valueOf(tokens[i]);            
        }
        return addr;
    }

    public static Multicaster<Listener, Message> synchronousGatewayMulticaster() {
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable task) {
                task.run();
            }
        };

        return new Multicaster<Listener, Message>(executor) {
            @Override
            protected void dispatch(Listener listener, Message message) {
                listener.onMessage(message);
            }
        };
    }

    public static void fakeSystemTime(final long time) {
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return time;
            }
        });
    }
}
