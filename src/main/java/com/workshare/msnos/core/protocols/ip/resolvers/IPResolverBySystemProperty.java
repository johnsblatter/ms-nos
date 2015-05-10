package com.workshare.msnos.core.protocols.ip.resolvers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.protocols.ip.AddressResolver;
import com.workshare.msnos.core.protocols.ip.Network;

public class IPResolverBySystemProperty implements IPResolver {

    private static Logger log = LoggerFactory.getLogger(IPResolverBySystemProperty.class);

    private final String systemPropertyName;

    public IPResolverBySystemProperty(String name) {
        systemPropertyName = name;
    }

    @Override
    public byte[] resolve(AddressResolver context) {
        final String value = System.getProperty(systemPropertyName);
        log.debug("Get system property {} value: {}", systemPropertyName, value);

        try {
            final byte[] address = Network.createAddressFromString(value);
            log.debug("Get address {} from value {}", address, value);
            return address;
        }
        catch (Exception ex) {
            log.debug("Cannot resolve system property address from value '{}'", value);
            return null;
        }
    }

}
