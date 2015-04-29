package com.workshare.msnos.soup.threading;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an asynchronous multicaster, allows you to send asynchronous
 * notifications to an arbitrary number of listeners, managed by this It's
 * thread safe, it uses internally an Executor to deliver notifications, but you
 * can provide your own instance.
 * An option for synchronous dispatching is also available trough a separate
 * list of listeners which is invoked without using the executor   
 * 
 * 
 * To use it, you need to extend this class and provide an implementation of the
 * dispatch(L listener, M message) method - it should be straightforward.
 * 
 * @author bossola
 * 
 * @param <L>
 *            The listener class
 * @param <M>
 *            The message class (the argument to notify)
 */
public abstract class Multicaster<L, M> {

    private static final Logger log = LoggerFactory.getLogger(Multicaster.class);
    public static final Executor THREADPOOL = ExecutorServices.newFixedDaemonThreadPool(Integer.getInteger("msnos.multicaster.threads.num", 5));

    private List<L> syncListeners = new CopyOnWriteArrayList<L>();
    private List<L> asyncListeners = new CopyOnWriteArrayList<L>();

    private Executor asyncExecutor;

    public Multicaster() {
        this(THREADPOOL);
    }

    public Multicaster(Executor executor) {
        this.asyncExecutor = executor;
    }

    public L addListener(L listener) {
        asyncListeners.add(listener);
        return listener;
    }

    public L addSynchronousListener(L listener) {
        syncListeners.add(listener);
        return listener;
    }

    public boolean removeListener(L listener) {
        return asyncListeners.remove(listener);
    }

    public boolean removeSynchronousListener(L listener) {
        return syncListeners.remove(listener);
    }

    public void dispatch(final M message) {
        if (log.isTraceEnabled()) {
            ThreadPoolExecutor pool = (ThreadPoolExecutor) asyncExecutor;
            int active = pool.getActiveCount();
            int queued = pool.getQueue().size();
            long total = pool.getTaskCount();
            log.trace(String.format("%d %d %d\n", active, queued, total));
        }

        notifySync(message);
        notifyAsync(message);
    }

    private void notifySync(final M message) {
        for (final L listener : syncListeners) {
            dispatch(listener, message);
        }
    }

    private void notifyAsync(final M message) {
        for (final L listener : asyncListeners) {
            asyncExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    dispatch(listener, message);
                }
            });
        }
    }

    protected abstract void dispatch(L listener, M message);
}
