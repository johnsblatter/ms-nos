package com.workshare.msnos.soup.threading;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an asynchronous multicaster, allows you to send asynchronous
 * notifications to an arbitrary number of listeners, managed by this
 * It's thread safe, it uses internally an Executor to deliver notifications,
 * but you can provide your own instance.
 * 
 * To use it, you need to extend this class and provide an implementation of the
 * dispatch(L listener, M message) method - it should be straightforward.
 * 
 * @author bossola
 *
 * @param <L>	The listener class
 * @param <M>	The message class (the argument to notify)
 */
public abstract class Multicaster<L,M> {
	
    private static final Logger log = LoggerFactory.getLogger(Multicaster.class);
    public static final Executor THREADPOOL = ExecutorServices.newFixedDaemonThreadPool(Integer.getInteger("msnos.multicaster.threads.num", 5));
            
	private Set<L> listeners = new HashSet<L>();
	private Executor executor;

	public Multicaster() {
		this(THREADPOOL);
	}

	public Multicaster(Executor executor) {
		this.executor = executor;
	}

	public L addListener(L listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}

		return listener;
	}

	public boolean removeListener(L listener) {
		synchronized (listeners) {
			return listeners.remove(listener);
		}
	}

	public void dispatch(final M message) {
	    if (log.isTraceEnabled()) {
    	    ThreadPoolExecutor pool = (ThreadPoolExecutor)executor;
    	    int active = pool.getActiveCount();
            int queued = pool.getQueue().size();
            long total = pool.getTaskCount();
            log.trace(String.format("%d %d %d\n", active, queued, total));
	    }
	    
		synchronized (listeners) {
			for (final L listener : listeners) {
				executor.execute(new Runnable(){
					@Override
					public void run() {
						dispatch(listener,message);
					}});
			}
		}
	}

	protected abstract void dispatch(L listener, M message)
	;
	
}
