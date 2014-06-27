package com.workshare.msnos.soup.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

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
    

    public static ExecutorService newFixedDaemonThreadPool(final int size) {
		return Executors.newFixedThreadPool(size, DAEMON_THREAD_FACTORY);
	}

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return Executors.newSingleThreadScheduledExecutor(DAEMON_THREAD_FACTORY);
    }
}
