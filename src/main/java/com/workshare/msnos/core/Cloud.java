package com.workshare.msnos.core;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.soup.json.Json;

public class Cloud {

	private static Logger log = LoggerFactory.getLogger(Cloud.class);
	
    private final Iden iden;
    private final Set<Agent> agents;
    private final transient Set<Gateway> gates;

    public Cloud(UUID uuid) throws IOException {
        this(uuid, Gateways.all());
    }

    public Cloud(UUID uuid, Set<Gateway> gates) throws IOException {
        this.iden = new Iden(Iden.Type.CLD, uuid);
		this.agents = new HashSet<Agent>();

		this.gates = gates;
		for (Gateway gate : gates) {
			gate.addListener(new Listener(){
				@Override
				public void onMessage(Message message) {
					process(message);
				}});
		}
    }

    protected void process(Message message) {
    	if (message.getType() == Message.Type.PRS) {
    		processPresence(message);
    	}
	}

	private void processPresence(Message message) {
		Iden from = message.getFrom();
		Agent agent = new Agent(from, this);
		synchronized(agents) {
			if (!agents.contains(agent)) {
				log.debug("Discovered new agent from network: {}", agent);
				agents.add(agent);
			}
		}
	}

	public Iden getIden() {
        return iden;
    }
    
    public String toString() {
        return Json.toJsonString(this);
    }

	public Set<Agent> getAgents() {
		return Collections.unmodifiableSet(agents);
	}

	public Set<Gateway> getGateways() {
		return Collections.unmodifiableSet(gates);
	}

	void onJoin(Agent agent) throws IOException {
		for (Gateway gate : gates) {
			gate.send(Messages.presence(agent, this));
		}
		
		synchronized(agents) {
			agents.add(agent);
		}
	}
}
