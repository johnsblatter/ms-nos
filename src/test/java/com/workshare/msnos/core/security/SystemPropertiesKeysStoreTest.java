package com.workshare.msnos.core.security;

import static org.junit.Assert.*;

import org.junit.Test;

public class SystemPropertiesKeysStoreTest {
    
    @Test
    public void shouldFailLookingForMissingKey() {
        System.setProperty(SystemPropertiesKeysStore.SYSP_KEYS, "123=ABC");

        KeysStore keys = new SystemPropertiesKeysStore();
        
        assertNull(keys.get("999"));
    }

    @Test
    public void shouldLoadKeyvalFromSingleSystemProperty() {
        System.setProperty(SystemPropertiesKeysStore.SYSP_KEYS, "123=ABC");

        KeysStore keys = new SystemPropertiesKeysStore();
        
        assertEquals("ABC", keys.get("123"));
    }
    
    @Test
    public void shouldLoadKeyvalFromMultipleSystemProperty() {
        System.setProperty(SystemPropertiesKeysStore.SYSP_KEYS, "123=ABC,777=BBB");

        KeysStore keys = new SystemPropertiesKeysStore();
        
        assertEquals("ABC", keys.get("123"));
        assertEquals("BBB", keys.get("777"));
    }   

    @Test
    public void shouldNotBlowupWhenInvalidMultiSystemProperty() {
        System.setProperty(SystemPropertiesKeysStore.SYSP_KEYS, "123=ABC,yuppidu");

        KeysStore keys = new SystemPropertiesKeysStore();
        
        assertEquals("ABC", keys.get("123"));
    }
    
    @Test
    public void shouldNotBlowupWhenInvalidSingleSystemProperty() {
        System.setProperty(SystemPropertiesKeysStore.SYSP_KEYS, "123");
        new SystemPropertiesKeysStore();
    }
    
    @Test
    public void shouldNotBlowupWhenPropertyNotPresent() {
        System.getProperties().remove(SystemPropertiesKeysStore.SYSP_KEYS);
        new SystemPropertiesKeysStore();
    }

    @Test
    public void shouldNotBeEmptyWhenValuesPresent() {
        System.setProperty(SystemPropertiesKeysStore.SYSP_KEYS, "123=ABC");

        KeysStore keys = new SystemPropertiesKeysStore();
        
        assertFalse(keys.isEmpty());
    }
    

    @Test
    public void shouldBeEmptyWhenPropertyNotPresent() {
        System.getProperties().remove(SystemPropertiesKeysStore.SYSP_KEYS);
        KeysStore keys = new SystemPropertiesKeysStore();
        
        assertTrue(keys.isEmpty());
    }
}
