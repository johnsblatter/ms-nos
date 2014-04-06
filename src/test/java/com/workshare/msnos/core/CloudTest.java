package com.workshare.msnos.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.protocols.ip.udp.UDPGateway;

public class CloudTest {

	private Cloud cloud;
	private Gateway gate;

	@Before
	public void init() throws Exception {
		gate = Mockito.mock(Gateway.class);
		cloud = new Cloud(UUID.randomUUID(), new HashSet<Gateway>(Arrays.asList(gate)));
	}
	
	@Test
	public void shouldCreateDefaultGateways() throws Exception {

		cloud = new Cloud(UUID.randomUUID());

		Set<Gateway> gates = cloud.getGateways();

		assertEquals(1, gates.size());
		assertEquals(UDPGateway.class, gates.iterator().next().getClass());
	}

	@Test
	public void shouldSendPresenceMessageWhenAgentJoins() throws Exception {
		Agent smith = new Agent(UUID.randomUUID());
		
		smith.join(cloud);
		
		Message message = getLastMessageSent();
		assertNotNull(message);
		assertEquals(Message.Type.PRS, message.getType());
		assertEquals(smith.getIden(), message.getFrom());
		assertEquals(cloud.getIden(), message.getTo());
	}

	@Test
	public void shouldUpdateAgentsListWhenAgentJoins() throws Exception {
		Agent smith = new Agent(UUID.randomUUID());
		
		smith.join(cloud);
		
		assertTrue(cloud.getAgents().contains(smith));
	}

	@Test
	public void shouldUpdateAgentsListWhenAgentJoinsTroughGateway() throws Exception {
		Agent frank = new Agent(UUID.randomUUID());

		simulateAgentJoiningCloud(frank);
		
		assertTrue(cloud.getAgents().contains(frank));
	}

	private void simulateAgentJoiningCloud(Agent agent) {
        ArgumentCaptor<Listener> gateListener = ArgumentCaptor.forClass(Listener.class);
        verify(gate).addListener(gateListener.capture());
        gateListener.getValue().onMessage(Messages.presence(agent, cloud));
	}

	private Message getLastMessageSent() throws IOException {
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(gate).send(captor.capture());
		return captor.getValue();
	}
}
