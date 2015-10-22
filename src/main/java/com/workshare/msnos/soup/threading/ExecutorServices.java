package com.workshare.msnos.soup.threading;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.workshare.msnos.soup.threading.ThreadFactories.Customizer;

/**
 * An enhanced executor service factory
 * 
 * @author bossola
 */
public class ExecutorServices {

	private static ThreadFactory DAEMON_THREAD_FACTORY= ThreadFactories.newCustomThreadFactory(new Customizer(){
        @Override
        public void apply(Thread thread) {
            thread.setDaemon(true);
        }});
    

    public static Executor newSynhcronousExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }};
    }


    public static ExecutorService newFixedDaemonThreadPool(final int size) {
        return new ThreadPoolExecutor(size, size,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                DAEMON_THREAD_FACTORY, 
                new ThreadPoolExecutor.CallerRunsPolicy());
	}

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return Executors.newSingleThreadScheduledExecutor(DAEMON_THREAD_FACTORY);
    }


    public static ExecutorService newCachedDaemonThreadPool() {
        return Executors.newCachedThreadPool(DAEMON_THREAD_FACTORY);
    }
}
