package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.json.Json;
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
        this(present, present ? getAllNetworks() : new HashSet<Network>());
    }

    Presence(boolean present, Set<Network> networks) {
        this.present = present;
        this.networks = networks;
        log.trace(present ? "Presence message created: {}" : "Absence message created: {}", this);
    }

    public boolean isPresent() {
        return present;
    }

    private static Set<Network> getAllNetworks() {
        Set<Network> nets = new HashSet<Network>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                nets.addAll(Network.list(nic, true));
            }
        } catch (SocketException e) {
            log.error("Socket Exception getting NIC info", e);
        }
        return nets;
    }

    public Set<Network> getNetworks() {
        return networks;
    }

    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
    public Message.Payload[] split() {
        Set<Network> netOne = new HashSet<Network>();
        Set<Network> netTwo = new HashSet<Network>();

        int i = 0;
        for (Network net : networks) {
            if (i++ % 2 == 0)
                netOne.add(net);
            else
                netTwo.add(net);
        }


        return new Payload[]{
                new Presence(present, netOne),
                new Presence(present, netTwo)
        };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + networks.hashCode();
        result = prime * result + (present ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Presence other = (Presence) obj;
            return networks.equals(other.networks) && present == other.present;
        } catch (Exception any) {
            return false;
        }
    }

    @Override
    public boolean process(Message message, Cloud.Internal internal) {
        Iden from = message.getFrom();

        RemoteAgent agent = new RemoteAgent(from.getUUID(), internal.cloud(), getNetworks());
        agent.setSeq(message.getSeq());

        if (isPresent()) {
            log.debug("Discovered new agent from network: {}", agent.toString());
            internal.remoteAgents().add(agent);
        } else {
            log.debug("Agent from network leaving: {}", from);
            internal.remoteAgents().remove(from);
        }

        return true;
    }


}
