package com.workshare.msnos.threading;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.junit.Test;

public class ExecutorServicesTest {

    @Test
    public void shouldCreateDaemonThreads() throws Exception {
        Future<Boolean> daemon = ExecutorServices.newFixedDaemonThreadPool(1).submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                return Thread.currentThread().isDaemon();
            }});
    
        assertTrue(daemon.get());
    }
    
}
