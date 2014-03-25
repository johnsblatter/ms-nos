package com.workshare.msnos.soup.threading;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * An enhanced thread factory that allows you to create customized thread
 * (i.e. force them to be daemon, including a name, a group...)
 * 
 * @author bossola
 */
public class ThreadFactories {

	public static final ThreadFactory DEFAULT = Executors.defaultThreadFactory();

	public static interface Customizer {
		public void apply(Thread thread);
	};

	public static final ThreadFactory newCustomThreadFactory(final Customizer customizer) {
		return new ThreadFactory () {
			@Override
			public Thread newThread(Runnable runnable) {
				Thread thread = DEFAULT.newThread(runnable);
				customizer.apply(thread);
				return thread;
			}
		};

	}
}
