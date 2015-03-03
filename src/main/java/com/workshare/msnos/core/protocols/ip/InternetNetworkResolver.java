package com.workshare.msnos.core.protocols.ip;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check if an interface is currently connected to the internet
 * (currently NOT used)
 * 
 * @author bossola
 */ 

public class InternetNetworkResolver  {
    
    public static final String SYSP_NET_REACHABLE_TIMEOUT = "com.ws.msnos.network.reachable.timeout.millis";

    private static final Logger log = LoggerFactory.getLogger(InternetNetworkResolver.class);

    private final int timeoutInMillis;

    public InternetNetworkResolver() {
        timeoutInMillis = Integer.getInteger(SYSP_NET_REACHABLE_TIMEOUT, 3000);
    }

    public boolean isReachable(NetworkInterface nic, InetAddress address) {

        try {
            if (!address.isReachable(timeoutInMillis))
                return false;
        }
        catch (IOException ex) {
            log.warn("Unexpected exception", ex);
        }
        
        Socket socket = new Socket();
        try {
            socket.setSoTimeout(3000);
            socket.bind(new InetSocketAddress(address, 0));
            socket.connect(new InetSocketAddress("google.com", 80), timeoutInMillis);
            log.info("Interface {}/{} accepted, internet is reachable", nic.getDisplayName(), address);
            return true;
        } catch (IOException ex) {
            log.info("Interface {}/{} DISCARDED: do not appear to be connected to the internet", nic.getDisplayName(), address);
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }
}
