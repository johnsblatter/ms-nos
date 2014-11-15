package com.workshare.msnos.core;

import com.workshare.msnos.core.protocols.ip.Network;

public class CoreHelper {

    private CoreHelper() {}
    
    public static Network asPublicNetwork(String host) {
        return asNetwork(host, (short)1);
    }

    public static Network asNetwork(String host, short prefix) {
        return new Network(toByteArray(host), prefix);
    }

    public static byte[] toByteArray(String host) {
        String[] tokens = host.split("\\.");
        byte[] addr = new byte[4];
        for (int i = 0; i < addr.length; i++) {
            addr[i] = Byte.valueOf(tokens[i]);            
        }
        return addr;
    }


}
