package com.workshare.msnos.core.protocols.ip;

import static java.lang.System.out;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;


public class Spike {


    public static void main(String[] args) throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets))
            displayInterfaceInformation(netint);
    }

    private static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
        out.printf("Display name: %s\n", netint.getDisplayName());
        out.printf("Name: %s\n", netint.getName());
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();

        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            out.printf("InetAddress: %s\n", inetAddress);
        }

        out.printf("Up? %s\n", netint.isUp());
        out.printf("Loopback? %s\n", netint.isLoopback());
        out.printf("PointToPoint? %s\n", netint.isPointToPoint());
        out.printf("Supports multicast? %s\n", netint.supportsMulticast());
        out.printf("Virtual? %s\n", netint.isVirtual());
        out.printf("Hardware address: %s\n",
                    Arrays.toString(netint.getHardwareAddress()));
        out.printf("MTU: %s\n", netint.getMTU());

        out.printf("\n");

    }

//  public static void main(String[] args) throws Exception {
//  Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
//  while (nics.hasMoreElements()) {
//      NetworkInterface nic = nics.nextElement();
//      System.out.println(nic.getDisplayName());
//      Set<Network> nets = Network.list(nic, true);
//      for (Network net : nets) {
//          System.out.println(" - " + net);
////          Endpoint ep = new BaseEndpoint(net, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), (short) 9999);
////          System.out.println(ep);
//      }
//  }
//}
    
}

