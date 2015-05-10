package com.workshare.msnos.core.protocols.ip.resolvers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.protocols.ip.AddressResolver;

public class CompositeIPResolver implements IPResolver {

    private static Logger log = LoggerFactory.getLogger(CompositeIPResolver.class);

    private final IPResolver[] solvers;

    public CompositeIPResolver(IPResolver... solvers) {
        this.solvers = solvers;
    }

    @Override
    public byte[] resolve(AddressResolver context) {
        for (IPResolver solver : solvers) {
            try {
                byte[] result = solver.resolve(context);
                if (result != null)
                    return result;
            } catch (Exception ex) {
                if (log.isTraceEnabled())
                    log.trace("Ops! A resolver bombed :) I will ignore him", ex);
                else
                    log.debug("Ops! A resolver bombed :) I will ignore him -> "+ex.getMessage());
            }
        }

        return null;
    }

}
