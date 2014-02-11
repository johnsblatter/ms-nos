package com.workshare.msnos.threading;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ThreadFactory;

import org.junit.Test;

import com.workshare.msnos.threading.TheadFactories.Customizer;

public class TheadFactoriesTest {

    @Test
    public void shouldCustomizeThread() {
        ThreadFactory factory = TheadFactories.newCustomThreadFactory(new Customizer(){

            @Override
            public void apply(Thread thread) {
                thread.setName("foo");
            }});
        
        Thread thread = factory.newThread(new Runnable(){
            @Override
            public void run() {
            }});

        assertEquals("foo", thread.getName());
    }
}
