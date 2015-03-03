package com.workshare.msnos.core.protocols.ip;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Network {

    public static final String SYSP_NET_BINDINGS = "com.ws.msnos.network.bindings";

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

    public static Set<Network> listAll(boolean ipv4only, boolean includeVirtual) {
        Enumeration<NetworkInterface> nics = null;
        try {
            nics = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            log.error("FATAL - Socket Exception getting NIC info", e);
            System.exit(-1);
        }

        return listAll(nics, ipv4only, includeVirtual, new AddressResolver());
    }

    static Set<Network> listAll(Enumeration<NetworkInterface> nics, boolean ipv4only, boolean includeVirtual, AddressResolver addresResolver) {
        String bindings = System.getProperty(SYSP_NET_BINDINGS);
        
        Set<Network> nets = new HashSet<Network>();
        while (nics.hasMoreElements()) {
            NetworkInterface nic = nics.nextElement();

            if (bindings != null && !bindings.contains(nic.getName())) {
                log.warn("Interface \"{}\" excluded as listed bindings are \"{}\"", nic.getName(), bindings);
                continue;
            }
                
            if (!includeVirtual && nic.isVirtual())
                continue;

            if (isLoopback(nic))
                continue;

            nets.addAll(list(nic, ipv4only));
        }

        try {
            log.debug("Trying to resolve a public address on the cloud...");
            Network publicIP = addresResolver.findPublicIP();
            if (publicIP != null)
                nets.add(publicIP);
        } catch (IOException e) {
            log.info("Unable to get a public IP for this machine", e);
            if (log.isDebugEnabled())
                log.debug("Unable to get a public IP", e);
        }
        return nets;
    }

    public static Set<Network> list(NetworkInterface nic, boolean ipv4Only) {

        Set<Network> lans = new HashSet<Network>();
        
        final List<InterfaceAddress> nicAddresses = nic.getInterfaceAddresses();
        for (InterfaceAddress nicAddress : nicAddresses) {
            if (nicAddress.getAddress().isLoopbackAddress())
                continue;

            final Network net = new Network(nicAddress);
            if (!net.isIpv4() && ipv4Only)
                continue;
            
            lans.add(net);
        }

        return lans;
    }

    private static boolean isLoopback(NetworkInterface nic) {
        try {
            return nic.isLoopback();
        } catch (SocketException e) {
            log.warn("Unable to determine if interface {} is a loopback", nic);
            return false;
        }
    }
}
