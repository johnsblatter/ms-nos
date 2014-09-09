package com.workshare.msnos.core.protocols.ip;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Set;

public class Spike {
    public static void main(String[] args) throws Exception {
        Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
        while (nics.hasMoreElements()) {
            NetworkInterface nic = nics.nextElement();
            System.out.println(nic.getDisplayName());
            Set<Network> nets = Network.list(nic, true);
            for (Network net : nets) {
//                System.out.println(" - " + net);
//                Endpoint ep = new Endpoint(net, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), (short) 9999);
//                System.out.println(ep);
            }
        }
    }
}
