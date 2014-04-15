package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.protocols.ip.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class Presence implements Message.Payload {

    private static Logger log = LoggerFactory.getLogger(Presence.class);

    private final boolean present;
    private final Set<Network> networks;

    public Presence(boolean present) {
        this.present = present;
        networks = present ? setNetworks() : new HashSet<Network>();
    }

    public boolean isPresent() {
        return present;
    }

    private Set<Network> setNetworks() {
        Set<Network> nets = new HashSet<Network>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                nets.addAll(Network.list(nic));
            }
        } catch (SocketException e) {
            log.error("Socket Exception getting NIC info", e);
        }
        return nets;
    }

    public Set<Network> getNetworks() {
        return networks;
    }

}
