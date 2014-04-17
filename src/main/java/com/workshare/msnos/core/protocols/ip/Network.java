package com.workshare.msnos.core.protocols.ip;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 
 * A simplified view of an IP network
 * 
 * @author bossola
 *
 */
public class Network {

	private final byte[] address;
	private final short prefix;

	public Network(byte[] address, short prefix) {
		this.address = address;
		this.prefix = prefix;
	}

    public Network(InterfaceAddress inetAddress)  {
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

	@Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < address.length; i++) {
            if (i > 0)
                sb.append('.');
            int x = (int) (address[i] & 0xff);
            sb.append(x);
        }
   
        sb.append('/');
        sb.append(prefix);

        return sb.toString();
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

    public static Set<Network> list(NetworkInterface nic, boolean ipv4Only) {
        Set<Network> lans = new HashSet<Network>();
        final List<InterfaceAddress> nicAddresses = nic.getInterfaceAddresses();
        for (InterfaceAddress nicAddress : nicAddresses) {
            if (!nicAddress.getAddress().isLoopbackAddress()) {
                final Network net = new Network(nicAddress);
                if (!ipv4Only || net.isIpv4())
                    lans.add(net);
            }
        }

        return lans;
    }
}
