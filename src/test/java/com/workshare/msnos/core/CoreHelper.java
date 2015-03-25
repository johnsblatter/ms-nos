package com.workshare.msnos.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.MsnosException.Code;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.Endpoints;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.threading.Multicaster;
import com.workshare.msnos.soup.time.SystemTime;

public class CoreHelper {

    private CoreHelper() {
    }

    public static void sleep(long duration, TimeUnit unit) {
        try {
            Thread.sleep(TimeUnit.MILLISECONDS.convert(duration, unit));
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new RuntimeException("wtf?");
        }
    }

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
        return asNetwork(host, (short) 1);
    }

    public static Network asNetwork(String host, short prefix) {
        return new Network(toByteArray(host), prefix);
    }

    public static byte[] toByteArray(String host) {
        String[] tokens = host.split("\\.");
        byte[] addr = new byte[4];
        for (int i = 0; i < addr.length; i++) {
            addr[i] = (byte) (Integer.valueOf(tokens[i]) & 0xff);
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

    public static void fakeSystemTime(final long timeInMillis) {
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return timeInMillis;
            }
        });
    }

    public static void fakeElapseTime(final long elapsedInMillis) {
        final long current = SystemTime.asMillis();
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return current + elapsedInMillis;
            }
        });
    }
    
    public static Endpoints makeImmutableEndpoints(final Set<Endpoint> all) {
        return makeEndpoints(Collections.unmodifiableSet(all));
    }

    public static Endpoints makeMutableEndpoints() {
        final Set<Endpoint> all = new HashSet<Endpoint>();
        return makeEndpoints(Collections.unmodifiableSet(all));
    }

    private static Endpoints makeEndpoints(final Set<Endpoint> all) {
        Endpoints endpoints = new Endpoints() {
            @Override
            public Set<? extends Endpoint> all() {
                return all;
            }

            @Override
            public Set<? extends Endpoint> publics() {
                return asSet();
            }

            @Override
            public Set<? extends Endpoint> of(Agent agent) {
                return asSet();
            }

            @Override
            public Endpoint install(Endpoint endpoint) throws MsnosException {
                throw new MsnosException("I am a test :)", Code.UNRECOVERABLE_FAILURE);
            }

            @Override
            public Endpoint remove(Endpoint endpoint) throws MsnosException {
                throw new MsnosException("I am a test :)", Code.UNRECOVERABLE_FAILURE);
            }
        };
        return endpoints;
    }

    public static com.workshare.msnos.core.cloud.Multicaster synchronousCloudMulticaster() {
        return new com.workshare.msnos.core.cloud.Multicaster(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        });
    }

    public static MessageBuilder newAPPMesage(Agent from, Identifiable to) {
        return new MessageBuilder(Message.Type.APP, from, to);
    }

    public static MessageBuilder newAPPMesage(Iden from, Iden to) {
        return new MessageBuilder(Message.Type.APP, from, to);
    }
}
