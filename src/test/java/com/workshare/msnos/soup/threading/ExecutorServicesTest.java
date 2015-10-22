package com.workshare.msnos.soup.threading;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Test;

public class ExecutorServicesTest {

    @Test
    public void shouldFixedThreadPoolCreateDaemonThreads() throws Exception {
        assertDaemonThreads(ExecutorServices.newFixedDaemonThreadPool(1));
    }

    @Test
    public void shouldCachedThreadPoolCreateDaemonThreads() throws Exception {
        assertDaemonThreads(ExecutorServices.newCachedDaemonThreadPool());
    }
    
    private void assertDaemonThreads(final ExecutorService pool) throws InterruptedException, ExecutionException {
        Future<Boolean> daemon = pool.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                return Thread.currentThread().isDaemon();
            }});
    
        assertTrue(daemon.get());
    }
    
}
