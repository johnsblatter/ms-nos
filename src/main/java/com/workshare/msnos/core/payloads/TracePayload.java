package com.workshare.msnos.core.payloads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.cloud.IdentifiablesList;
import com.workshare.msnos.soup.json.Json;

public class TracePayload implements Message.Payload {

    private static Logger log = LoggerFactory.getLogger(TracePayload.class);

    public static class Crumb {
        private final UUID src;
        private final UUID dst;
        private final String way;
        private final int hop;

        private Crumb(UUID src, UUID dst, Gateway gate, int hops) {
            if (src == null || dst == null || gate == null)
                throw new IllegalArgumentException("No nulls accepted here!");

            this.src = src;
            this.dst = dst;
            this.way = gate.name();
            this.hop = hops;
        }

        public UUID source() {
            return src;
        }

        public UUID destination() {
            return dst;
        }

        public String way() {
            return way;
        }

        public int hops() {
            return hop;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + hop;
            result = prime * result + ((src == null) ? 0 : src.hashCode());
            result = prime * result + ((dst == null) ? 0 : dst.hashCode());
            result = prime * result + ((way == null) ? 0 : way.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            try {
                Crumb other = (Crumb) obj;
                if (!src.equals(other.src))
                    return false;
                if (!dst.equals(other.dst))
                    return false;
                if (!way.equals(other.way))
                    return false;
                if (hop != other.hop)
                    return false;
                
                return true;
            } catch (Exception any) {
                return false;
            }
        }
        
        
    }
    
    private final Iden from;
    private final List<Crumb> crumbs;
    
    TracePayload(Iden from, List<Crumb> someCrumbs) {
        if (from == null || someCrumbs == null)
            throw new IllegalArgumentException("No nulls accepted here!");
        
        this.from = from;
        this.crumbs = Collections.unmodifiableList(someCrumbs);
    }

    public TracePayload(Iden from) {
        this(from, Collections.<Crumb>emptyList());
    }

    public TracePayload crumbed(UUID src, UUID dst, Gateway gate, int hops) {
        List<Crumb> newCrumbs = new ArrayList<Crumb>(crumbs.size()+1);
        newCrumbs.add(new Crumb(src, dst, gate, hops));
        return new TracePayload(this.from, newCrumbs);
    }
    
    public Iden from() {
        return from;
    }

    @Override
    public Payload[] split() {
        List<Crumb> one = new ArrayList<Crumb>();
        List<Crumb> two = new ArrayList<Crumb>();

        int i = 0;
        for (Crumb crumb : crumbs) {
            if (i++%2 == 0)
                one.add(crumb);
            else
                two.add(crumb);
        }
        
        return new Payload[] {
            new TracePayload(from, one),
            new TracePayload(from, two)
        };
    }

    @Override
    public boolean process(Message message, Internal internal) {
        
        Iden to = message.getTo();
        final IdentifiablesList<LocalAgent> locals = internal.localAgents();
        if (locals.containsKey(to)) {
            final Message answer = new MessageBuilder(Message.Type.CRT, to, this.from()).with(this).make();
            try {
                internal.cloud().send(answer);
            } catch (MsnosException ex) {
                log.error("Unexpected exception while trasmitting message "+answer, ex);
            }
        }

        return false;
    }

    public List<Crumb> crumbs() {
        return crumbs;
    }
    
    @Override
    public String toString() {
        return Json.toJsonString(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((crumbs == null) ? 0 : crumbs.hashCode());
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            TracePayload other = (TracePayload) obj;
            if (!from.equals(other.from))
                return false;
            
            if (crumbs.size() != other.crumbs.size())
                return false;
            
            for (int i = 0; i < crumbs.size(); i++) {
                Crumb self = crumbs.get(i);
                Crumb othr = other.crumbs.get(i);
                if (!self.equals(othr))
                    return false;
            }
            
            return true;
        } catch (Exception any) {
            return false;
        }
    }
    
    
}

