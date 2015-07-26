package com.workshare.msnos.soup.threading;

import static com.workshare.msnos.core.cloud.CoreHelper.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class MulticasterTest {

    private StringBuffer trace;

    private Multicaster<Observer, Object> caster;
    private ExecutorService executor;

    @Before
    public void init() {
        trace = new StringBuffer();
    }

    @Test
    public void shouldInvokeAListeners() {
        caster().addListener(newTraceAppendingListener());

        caster().dispatch("one");
        caster().dispatch("two");

        assertTraceContains("one");
        assertTraceContains("two");
    }

    @Test
    public void shouldInvokeAllListeners() {
        caster().addListener(newTraceAppendingListener("alfa"));
        caster().addListener(newTraceAppendingListener("beta"));

        caster().dispatch("one");
        caster().dispatch("two");

        assertTraceContains("alfaone");
        assertTraceContains("alfatwo");
        assertTraceContains("betaone");
        assertTraceContains("betatwo");
    }

    @Test
    public void shouldRemoveListeners() {
        Observer beta = newTraceAppendingListener("beta");
        caster().addListener(beta);
        caster().addListener(newTraceAppendingListener("alfa"));
        caster().removeListener(beta);

        caster().dispatch("one");
        caster().dispatch("two");

        assertTraceContains("alfaone");
        assertTraceContains("alfatwo");
        assertTraceNotContains("betaone");
        assertTraceNotContains("betatwo");
    }

    @Test
    public void shouldWorkAsynchronously() throws Exception {
        executor = ExecutorServices.newFixedDaemonThreadPool(3);

        caster().addListener(newTraceAppendingListener("AAA"));
        final String name = "XYZ";
        final long delay = 250l;
        caster().addListener(new Observer() {
            @Override
            public void update(Observable o, Object event) {
                sleep(delay, TimeUnit.MILLISECONDS);
                trace.append(name+event);
            }
        });

        caster().dispatch("one");
        sleep(100l, TimeUnit.MILLISECONDS);
        caster().dispatch("two");

        shutdown(executor);
        assertEquals("AAAoneAAAtwoXYZoneXYZtwo", trace.toString());
    }

    @Test
    public void shouldInvokePiorityListenersBeforeNormalListeners() throws Exception {
        executor = ExecutorServices.newFixedDaemonThreadPool(3);

        caster().addSynchronousListener(newTraceAppendingListener("PRIORITY", 200l));
        caster().addListener(newTraceAppendingListener("-STANDARD", 0l));
        caster().addListener(newTraceAppendingListener("-STANDARD", 0l));

        caster().dispatch("");

        shutdown(executor);
        assertEquals("PRIORITY-STANDARD-STANDARD", trace.toString());
    }

    private void shutdown(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    private Multicaster<Observer, Object> caster() {
        if (caster == null)
            caster = new Multicaster<Observer, Object>(executor()) {
                @Override
                protected void dispatch(Observer listener, Object message) {
                    listener.update(null, message);
                }
            };

        return caster;
    }

    private void assertTraceContains(final String text) {
        assertTrue(trace.indexOf(text) != -1);
    }

    private void assertTraceNotContains(final String text) {
        assertTrue(trace.indexOf(text) == -1);
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

    private Observer newTraceAppendingListener() {
        return newTraceAppendingListener(null, 0l);
    }

    private Observer newTraceAppendingListener(final String name) {
        return newTraceAppendingListener(name, 0l);
    }

    private Observer newTraceAppendingListener(final String name, final long delay) {
        return new Observer() {
            @Override
            public void update(Observable o, Object message) {
                if (delay != 0l)
                    sleep(delay, TimeUnit.MILLISECONDS);

                if (name == null)
                    trace.append(message);
                else
                    trace.append(name + message);
            }
        };
    }

}
