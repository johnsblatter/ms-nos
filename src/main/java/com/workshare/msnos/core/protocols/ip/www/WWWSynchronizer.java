package com.workshare.msnos.core.protocols.ip.www;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.soup.threading.Multicaster;

public class WWWSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(WWWSynchronizer.class);

    public class Processor {
        private final Cloud cloud;
        private final Map<Iden, Message> messages = new HashMap<Iden, Message>();

        public Processor(Cloud acloud) {
            this.cloud = acloud;
        }

        public void accept(Message message) {
            final Iden from = message.getFrom();

            if (cloud.containsLocalAgent(from)) {
                log.debug("Skipping message {} for local agent", message);
                return;
            }
            
            if (isPresence(message, true)) {
                log.debug("Added presence message {}", message);
                messages.put(from, message);
            }
            else if (isPresence(message, false)) {
                log.debug("Removed by negative presence message {}", message);
                messages.remove(from);
            }
            else if (isFault(message)) {
                log.debug("Removed by negative presence message {}", message);
                messages.remove(getTarget(message));
            }
            else if (from.getType() == Iden.Type.AGT) {
                if (messages.get(from) == null) {
                    log.debug("Added fake presence message {} due to activity", message);
                    messages.put(from, newPresence(from));
                }
            }
        }

        private boolean isPresence(Message message, boolean present) {
            return message.getType() == Type.PRS && ((Presence)message.getData()).isPresent() == present;
        }

        private boolean isFault(Message message) {
            return message.getType() == Type.FLT;
        }

        public Iden getTarget(Message message) {
            if (message.getType() == Type.FLT) {
                return ((FltPayload)message.getData()).getAbout();
            } else
                return message.getFrom();
        }
        
        public void commit() {
            for (Message message : messages.values()) {
                if (message.getType() == Type.PRS) {
                    if (((Presence)message.getData()).getEndpoints() == RemoteAgent.NO_ENDPOINTS) {
                        try {
                            cloud.send(newDiscovery(message.getFrom()));
                        } catch (MsnosException e) {
                            log.warn("Unexpected exception sending discovery message", e);
                        }
                        
                        continue;
                    }
                }

                caster.dispatch(message);
            }
        }

        private Message newPresence(Iden from) {
            return new MessageBuilder(Message.Type.PRS, 
                    from, 
                    cloud.getIden()).with(new Presence(true, RemoteAgent.NO_ENDPOINTS)).make();
        }

        private Message newDiscovery(Iden to) {
            return new MessageBuilder(Message.Type.DSC, 
                    cloud.getIden(), 
                    to).make();
        }
    }
    
    private final Multicaster<Listener, Message> caster;

    public WWWSynchronizer(Multicaster<Listener, Message> caster) {
        this.caster = caster;
    }

    public Processor init(Cloud cloud) {
        return new Processor(cloud);
    }

    
//    public Message makePresence(Cloud cloud, Iden iden) {
//        return new MessageBuilder(Message.Type.PRS, iden, cloud).with(new Presence(true, iden)).make();
//    }
}
