package com.workshare.msnos.core.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class SystemPropertiesKeysStore implements KeysStore {

    private static Logger logger = Logger.getLogger(SystemPropertiesKeysStore.class);
    
    public static final String SYSP_KEYS = "com.ws.msnos.keys";

    public final Map<String,String> keyvals = new ConcurrentHashMap<String,String>();
    
    public SystemPropertiesKeysStore() {
        String property = System.getProperty(SYSP_KEYS);
        if (property != null) {
            String[] allvals = property.split(",");
            for (String keyval : allvals) {
                parseKeyVal(keyval);
            }
        }
    }

    private void parseKeyVal(String keyval) {
        try {
            String[] tokens = keyval.split("=");
            String key = tokens[0];
            String val = tokens[1];
            keyvals.put(key, val);
        }
        catch (Exception ex) {
            logger.warn("Unexpect error parsing keys from system property: '"+System.getProperty(SYSP_KEYS)+"'", ex);
        }
    }

    @Override
    public String get(String id) {
        return keyvals.get(id);
    }

    @Override
    public boolean isEmpty() {
        return keyvals.isEmpty();
    }

}
