package com.workshare.msnos.core.protocols.ip.www;

import static com.workshare.msnos.core.CoreHelper.makeEndpoints;
import static com.workshare.msnos.core.CoreHelper.newCloudIden;
import static com.workshare.msnos.core.CoreHelper.randomUUID;
import static com.workshare.msnos.core.MessagesHelper.newFaultMessage;
import static com.workshare.msnos.core.MessagesHelper.newPresenceMessage;
import static com.workshare.msnos.core.MessagesHelper.newQNEMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.CoreHelper;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Gateway.Listener;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.RemoteAgent;
import com.workshare.msnos.core.Ring;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.Endpoint;
import com.workshare.msnos.core.protocols.ip.www.WWWSynchronizer.Processor;
import com.workshare.msnos.soup.threading.Multicaster;

@SuppressWarnings("unchecked")
public class WWWSynchronizerTest {

    private Cloud cloud;
    private WWWSynchronizer synchro;
    private Multicaster<Listener, Message> caster;

    private RemoteAgent smith;

    private LocalAgent local;

    protected List<Message> messagesSent;
    
    @Before
    public void setup() throws Exception {
        Gateway gate = mock(Gateway.class);
        when(gate.endpoints()).thenReturn(makeEndpoints(CoreHelper.<Endpoint>asSet()));

        messagesSent = new ArrayList<Message>();

        cloud = Mockito.mock(Cloud.class);
        when(cloud.getIden()).thenReturn(newCloudIden());
        when(cloud.getRing()).thenReturn(Ring.random());

        caster = mock(Multicaster.class);
        synchro = new WWWSynchronizer(caster);
        
        smith = new RemoteAgent(randomUUID(), cloud, CoreHelper.<Endpoint>asSet() );
        
        local = new LocalAgent(randomUUID());
        local.join(cloud);
        when(cloud.containsLocalAgent(local.getIden())).thenReturn(true);
    }

    @Test
    public void shouldNotRoutePresenceMessageWhenAgentJoined() throws Exception {
        Processor proc = synchro.init(cloud);
        proc.accept(newPresenceMessage(smith, true));
        
        proc.commit();
        
        assertEquals(1, messagesRouted().size());
        assertPresenceRouted(smith);
    }

    @Test
    public void shouldNotRoutePresenceMessageWhenAgentJoinedAndThehLeft() throws Exception {
        Processor proc = synchro.init(cloud);
        proc.accept(newPresenceMessage(smith, true));
        proc.accept(newPresenceMessage(smith, false));
        
        proc.commit();
        
        assertEquals(0, messagesRouted().size());
    }

    @Test
    public void shouldNotRoutePresenceMessageWhenAgentJoinedAndThenFaulted() throws Exception {
        Processor proc = synchro.init(cloud);
        proc.accept(newPresenceMessage(smith, true));
        proc.accept(newFaultMessage(smith));
        
        proc.commit();
        
        assertEquals(0, messagesRouted().size());
    }


    @Test
    public void shouldRoutePresenceMessageWhenAgentSomehowActed() throws Exception {
        Processor proc = synchro.init(cloud);
        proc.accept(newQNEMessage(smith, "foo"));
        
        proc.commit();
        
        assertEquals(1, messagesRouted().size());
        assertPresenceRouted(smith);
    }

    @Test
    public void shouldRoutePresenceMessageWithoutOverridingExistingPresence() throws Exception {
        final Message presence = newPresenceMessage(smith, true);
        Processor proc = synchro.init(cloud);
        proc.accept(presence);
        proc.accept(newQNEMessage(smith, "foo"));
        
        proc.commit();
        
        assertEquals(1, messagesRouted().size());
        assertEquals(presence, messagesRouted().get(0));
    }
    
    @Test
    public void shouldNotProcessPresenceForLocalAgents() throws Exception {
        Processor proc = synchro.init(cloud);
        proc.accept(newPresenceMessage(local, true));
        
        proc.commit();
        
        assertEquals(0, messagesRouted().size());
    }
    
    @Test
    public void shouldSendDiscoveryToCloudForAgentsWithNoEndpoints() throws Exception {
        Processor proc = synchro.init(cloud);
        proc.accept(newQNEMessage(smith, "foo"));
        
        proc.commit();
        
        assertDiscoverySent(smith);
    }
    
    private void assertDiscoverySent(RemoteAgent agent) throws MsnosException {
        final List<Message> messages = messagesSent();
        for (Message message : messages) {
            if (message.getType() == Message.Type.DSC && message.getTo().equals(agent.getIden()))
                return;
        }
        
        fail("Expected discovery message not found!");
    }

    private void assertPresenceRouted(RemoteAgent agent) {
        final List<Message> messages = messagesRouted();
        for (Message message : messages) {
            if (isPresenceMessage(message, agent, true))
                return;
        }
        
        fail("Expected presence message not found!");
    }

    private boolean isPresenceMessage(Message message, RemoteAgent agent, boolean present) {
        if (!message.getFrom().equals(agent.getIden()))
            return false;

        if (message.getType() != Message.Type.PRS)
            return false;
        
        Presence payload = (Presence)message.getData();
        return present == payload.isPresent();
    }

    private List<Message> messagesRouted() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(caster, atMost(Integer.MAX_VALUE)).dispatch(captor.capture());
        return captor.getAllValues();
    }
    
    private List<Message> messagesSent() throws MsnosException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(cloud, atMost(Integer.MAX_VALUE)).send(captor.capture());
        return captor.getAllValues();
    }

}
