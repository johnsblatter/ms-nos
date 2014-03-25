package com.workshare.msnos.soup.threading;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.soup.threading.ExecutorServices;
import com.workshare.msnos.soup.threading.Multicaster;

public class MulticasterTest {

    private String dataOne;
    private String dataTwo;

    private Multicaster<Observer, Object> caster;
    private ExecutorService executor;
    private List<Observer> listeners;

    @Before
    public void init() {
        dataOne = "";
        dataTwo = "";

        caster = null;
    }

    @Test
    public void shouldInvokeAllListeners() {
        caster().dispatch("one");
        caster().dispatch("two");

        assertEquals("onetwo", dataOne);
        assertEquals("onetwo", dataTwo);
    }

    @Test
    public void shouldWorkAsynchronously() throws Exception {
        executor = ExecutorServices.newFixedDaemonThreadPool(2);
        
        final AtomicInteger count = new AtomicInteger(0);
        final StringBuffer dataThree = new StringBuffer();
        caster().addListener(new Observer() {
            @Override
            public void update(Observable o, Object event) {
                if (count.incrementAndGet() == 1) {
                    sleep(200l);
                }
                dataThree.append(event);
            }
        });

        caster().dispatch("one");
        caster().dispatch("two");

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        assertEquals("onetwo", dataOne);
        assertEquals("onetwo", dataTwo);
        assertEquals("twoone", dataThree.toString());
    }

    @Test
    public void shouldRemoveListeners() {
        caster().removeListener(listeners.get(0));
        
        caster().dispatch("one");
        caster().dispatch("two");

        assertEquals("", dataOne);
        assertEquals("onetwo", dataTwo);
    }

    private Multicaster<Observer, Object> caster() {
        if (caster != null)
            return caster;

        caster = new Multicaster<Observer, Object>(executor()) {
            @Override
            protected void dispatch(Observer listener, Object message) {
                listener.update(null, message);
            }
        };
        
        listeners = new ArrayList<Observer>();
        listeners.add(caster.addListener(new Observer() {
            @Override
            public void update(Observable o, Object event) {
                dataOne += event;
            }
        }));

        listeners.add(caster.addListener(new Observer() {
            @Override
            public void update(Observable o, Object event) {
                dataTwo += event;
            }
        }));

        return caster;
    }

    private Executor executor() {
        if (executor != null)
            return executor;

        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
}
