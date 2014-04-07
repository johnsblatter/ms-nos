package com.workshare.msnos.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.soup.json.Json;

public class Cloud implements Identifiable {

    public static interface Listener {
        public void onMessage(Message message)
        ;
    }
    
    public static class Multicaster extends com.workshare.msnos.soup.threading.Multicaster<Listener, Message> {
        public Multicaster() {
            super();
        }
        
        public Multicaster(Executor executor) {
            super(executor);
        }
        
        @Override
        protected void dispatch(Listener listener, Message message) {
            listener.onMessage(message);
        }
    }
    
	private static Logger log = LoggerFactory.getLogger(Cloud.class);
	
    private final Iden iden;
    private final Map<Iden, Agent> agents;

    transient private final Set<Gateway> gates;
    transient private final Multicaster caster;


    public Cloud(UUID uuid) throws IOException {
        this(uuid, Gateways.all(), new Multicaster());
    }

    public Cloud(UUID uuid, Set<Gateway> gates) throws IOException {
        this(uuid, gates, new Multicaster());
    }
   
    public Cloud(UUID uuid, Set<Gateway> gates, Multicaster multicaster) throws IOException {
        this.iden = new Iden(Iden.Type.CLD, uuid);
		this.agents = new HashMap<Iden, Agent>();
		this.caster = multicaster;

		this.gates = gates;
		for (Gateway gate : gates) {
			gate.addListener(new Gateway.Listener(){
				@Override
				public void onMessage(Message message) {
					process(message);
				}});
		}
    }

    public Iden getIden() {
        return iden;
    }
    
    public String toString() {
        return Json.toJsonString(this);
    }

    public void addListener(com.workshare.msnos.core.Cloud.Listener listener) {
        caster.addListener(listener);
    }
    
    public Collection<Agent> getAgents() {
        return Collections.unmodifiableCollection(agents.values());
    }

    public Set<Gateway> getGateways() {
        return Collections.unmodifiableSet(gates);
    }

    protected void process(Message message) {
        if (!this.iden.equals(message.getTo()) && !agents.containsKey(message.getTo())) {
            log.debug("Skipped message sent to another cloud: {}", message);
            return;
        }
        
    	if (message.getType() == Message.Type.PRS) {
    		processPresence(message);
    	} else {
    	    caster.dispatch(message);
    	}
	}

	private void processPresence(Message message) {
		Iden from = message.getFrom();
		Agent agent = new Agent(from, this);
		synchronized(agents) {
			if (!agents.containsKey(agent.getIden())) {
				log.debug("Discovered new agent from network: {}", agent);
				agents.put(agent.getIden(), agent);
			}
		}
	}

	void onJoin(Agent agent) throws IOException {
		for (Gateway gate : gates) {
			gate.send(Messages.presence(agent, this));
		}
		
		synchronized(agents) {
            agents.put(agent.getIden(), agent);
		}
	}

}
