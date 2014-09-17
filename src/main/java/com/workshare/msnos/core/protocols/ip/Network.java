package com.workshare.msnos.core.protocols.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Network {

    private static Logger log = LoggerFactory.getLogger(Network.class);

    private final byte[] address;
    private final short prefix;

    public Network(byte[] address, short prefix) {
        this.address = address;
        this.prefix = prefix;
    }

    public Network(InterfaceAddress inetAddress) {
        this(inetAddress.getAddress().getAddress(), inetAddress.getNetworkPrefixLength());
    }

    public boolean isIpv4() {
        return 4 == this.address.length;
    }

    public byte[] getAddress() {
        return this.address;
    }

    public short getPrefix() {
        return this.prefix;
    }

    public boolean isPrivate() {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            log.error("Unknown host exception when checking if an address is private ", e);
        }
        return inetAddress.isSiteLocalAddress();
    }

    public byte[] getNetmask() {
        byte[] netmask = new byte[address.length];
        int fullmask = prefix > 0 ? 0x00 - (1 << ((8 * address.length) - prefix)) : 0xFFFFFFFF;
        for (int i = 0; i < address.length; i++) {
            int shift = 8 * (address.length - i - 1);
            int bytemask = fullmask;
            bytemask >>= shift;
            bytemask &= 0xff;
            netmask[i] = (byte) bytemask;
        }

        return netmask;
    }


    public String getHostString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < address.length; i++) {
            if (i > 0)
                sb.append('.');
            int x = (int) (address[i] & 0xff);
            sb.append(x);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getHostString() + "/" + prefix;
    }

    // TODO: dumb implementation, should be improved
    @Override
    public int hashCode() {
        int tot = prefix;
        for (byte b : address) {
            int i = 1 + b;
            tot *= i;
        }
        return tot;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Network other = (Network) obj;
            if (address.length != other.address.length)
                return false;

            if (prefix != other.prefix)
                return false;

            for (int i = 0; i < address.length; i++) {
                if (address[i] != other.address[i])
                    return false;
            }
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    public static Set<Network> listAll(boolean ipv4only) {
        Set<Network> nets = new HashSet<Network>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                nets.addAll(list(nic, ipv4only));
            }
        } catch (SocketException e) {
            log.error("Socket Exception getting NIC info", e);
        }
        return nets;
    }

    public static Set<Network> list(NetworkInterface nic, boolean ipv4Only) {
        return list(nic, ipv4Only, new AddressResolver());
    }

    public static Set<Network> list(NetworkInterface nic, boolean ipv4Only, AddressResolver resolver) {
        Set<Network> lans = new HashSet<Network>();
        final List<InterfaceAddress> nicAddresses = nic.getInterfaceAddresses();
        for (InterfaceAddress nicAddress : nicAddresses) {
            if (!nicAddress.getAddress().isLoopbackAddress()) {
                final Network net = new Network(nicAddress);
                if (!ipv4Only || net.isIpv4()) {
                    if (net.isPrivate())
                        try {
                            Network publicIP = resolver.findPublicIP();
                            if (publicIP != null)
                                lans.add(publicIP);
                        } catch (IOException e) {
                            log.error("IOException trying to find public IP ", e);
                        }
                    lans.add(net);
                }
            }
        }
        return lans;
    }
}
