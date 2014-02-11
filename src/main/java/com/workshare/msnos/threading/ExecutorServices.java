package com.workshare.msnos.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.workshare.msnos.threading.TheadFactories.Customizer;

/**
 * An enhanced executor service factory
 * 
 * @author bossola
 */
public class ExecutorServices {

	public static ExecutorService newFixedDaemonThreadPool(final int size) {

		ThreadFactory daemonFactory = TheadFactories.newCustomThreadFactory(new Customizer(){
			@Override
			public void apply(Thread thread) {
				thread.setDaemon(true);
			}});
		
		return Executors.newFixedThreadPool(size, daemonFactory);
	}


}
