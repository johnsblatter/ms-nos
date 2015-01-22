package com.workshare.msnos.soup;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.soup.ShutdownHooks.Hook;

public class ShutdownHooksTest {

    private StringBuffer trace = new StringBuffer();

    @Before
    public void cleanup() {
        ShutdownHooks.clearAll();
    }
    
    @Test
    public void shouldExecuteWithPriority() {
        ShutdownHooks.addHook(newHook("one", 100));
        ShutdownHooks.addHook(newHook("two", 0));
        ShutdownHooks.addHook(newHook("tre", -100));
        
        ShutdownHooks.onShutdown();

        assertEquals("onetwotre", trace.toString());
    }

    @Test
    public void shouldAllowToRemoveHooks() {
        ShutdownHooks.addHook(newHook("one", 100));
        ShutdownHooks.addHook(newHook("two", 0));
        Hook hook = ShutdownHooks.addHook(newHook("tre", -100));
        ShutdownHooks.removeHook(hook);
        
        ShutdownHooks.onShutdown();

        final String tracestring = trace.toString();
        assertTrue(tracestring.contains("one"));
        assertTrue(tracestring.contains("two"));
        assertFalse(tracestring.contains("tre"));
    }

    private Hook newHook(final String name, final int prio) {
        return new Hook() {

            @Override
            public void run() {
                trace.append(name);
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public int priority() {
                return prio;
            }};
    }
    
}
