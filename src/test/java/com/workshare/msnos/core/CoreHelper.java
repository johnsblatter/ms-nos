package com.workshare.msnos.core;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.MsnosException.Code;
import com.workshare.msnos.core.cloud.IdentifiablesList;
import com.workshare.msnos.core.cloud.MessageValidators;
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
    
    public static AtomicLong fakeSystemTime() {
        return fakeSystemTime(0l);
    }
    
    public static AtomicLong fakeSystemTime(final long timeInMillis) {
        final AtomicLong counter = new AtomicLong();
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                if (timeInMillis == 0l)
                    return System.currentTimeMillis();
                else
                    return timeInMillis;
            }

            public void sleep(long millis) throws InterruptedException {
                Thread.sleep(millis);
                counter.addAndGet(millis);
            }
        });
        
        return counter;
    }

    public static void fakeElapseTime(final long elapsedInMillis) {
        final long current = SystemTime.asMillis();
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return current + elapsedInMillis;
            }

            public void sleep(long millis) throws InterruptedException {
                Thread.sleep(millis);
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

    public static Cloud createMockCloud() {
        return createMockCloud(null, null);
    }
    
    public static Cloud createMockCloud(final Iden iden, final Ring ring) {
        Cloud cloud = mock(Cloud.class);
        
        when(cloud.getIden()).thenReturn(iden == null ? newCloudIden() : iden);
        when(cloud.getRing()).thenReturn(ring == null ? Ring.random() : ring);

        Internal internal = mock(Cloud.Internal.class);
        when(internal.sign(any(Message.class))).thenAnswer(new Answer<Message>(){
            @Override
            public Message answer(InvocationOnMock invocation) throws Throwable {
                return (Message) invocation.getArguments()[0];
            }});

        when(internal.localAgents()).thenReturn(new IdentifiablesList<LocalAgent>());
        when(internal.remoteAgents()).thenReturn(new IdentifiablesList<RemoteAgent>());
        when(internal.remoteClouds()).thenReturn(new IdentifiablesList<RemoteEntity>());
        when(internal.cloud()).thenReturn(cloud);
        
        when(cloud.internal()).thenReturn(internal);
        when(cloud.validators()).thenReturn(new MessageValidators(internal));
        
        return cloud;
    }
    
    public static Cloud.Internal getCloudInternal(Cloud cloud) {
        return cloud.internal();
    }
}
