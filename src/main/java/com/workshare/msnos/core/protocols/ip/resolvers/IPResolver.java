package com.workshare.msnos.core.protocols.ip.resolvers;

import com.workshare.msnos.core.protocols.ip.AddressResolver;

public interface IPResolver {
    
    byte[] resolve(AddressResolver context)
    ;
}
