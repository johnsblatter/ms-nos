package com.workshare.msnos.core.protocols.ip.resolvers;

import com.workshare.msnos.core.protocols.ip.AddressResolver;

public class IPResolverByURL implements IPResolver {

    private final String url;

    public IPResolverByURL(String url) {
        this.url = url;
    }
    
    @Override
    public byte[] resolve(AddressResolver context) {
        return context.getIPViaURL(url);
    }
}
