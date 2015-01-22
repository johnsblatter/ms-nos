package com.workshare.msnos.soup;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHooks {
    private static Logger log = LoggerFactory.getLogger(ShutdownHooks.class);

    public static interface Hook extends Runnable {
        public String name();

        public int priority();
    }

    public static Set<Hook> hooks = Collections.<Hook> synchronizedSet(new TreeSet<Hook>(new Comparator<Hook>() {
        @Override
        public int compare(Hook h1, Hook h2) {
            final int pri2 = h2.priority();
            final int pri1 = h1.priority();
            return (pri1 < pri2 ? -1 : (pri1 == pri2 ? 0 : 1));
        }
    }));

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                onShutdown();
            }
        });
    }

    public static void addHook(Hook hook) {
        hooks.add(hook);
    }

    private static void onShutdown() {
        synchronized (hooks) {
            for (Hook hook : hooks) {
                try {
                    log.debug("Running hook: {}", hook.name());
                    hook.run();
                } catch (Throwable ex) {
                    log.warn("Unexpected exception while running hook " + hook, ex);
                }

            }
        }

    }
}
