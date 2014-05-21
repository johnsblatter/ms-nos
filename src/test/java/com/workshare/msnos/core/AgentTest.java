package com.workshare.msnos.core;

import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.protocols.ip.Network;
import com.workshare.msnos.soup.time.SystemTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.workshare.msnos.core.Message.Type.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AgentTest {

    private Cloud cloud;
    private LocalAgent karl;
    private LocalAgent smith;

    @Before
    public void before() throws Exception {
        cloud = mock(Cloud.class);

        karl = new LocalAgent(UUID.randomUUID());
        karl.join(cloud);

        smith = new LocalAgent(UUID.randomUUID());
        smith.join(cloud);
    }

    @After
    public void after() throws Exception {
        SystemTime.reset();
    }

    @Test
    public void agentShouldAttachListenerToCloud() {
        verify(cloud, atLeastOnce()).addListener(any(Cloud.Listener.class));
    }

    @Test
    public void shouldSendPresenceWhenDiscoveryIsReceived() throws IOException {
        simulateMessageFromCloud(Messages.discovery(cloud, smith));
        Message message = getLastMessageToCloud();

        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(PRS, message.getType());
    }

    @Test
    public void shouldSendPongWhenPingIsReceived() throws IOException {
        simulateMessageFromCloud(Messages.ping(cloud, smith));
        Message message = getLastMessageToCloud();

        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(PON, message.getType());
    }

    @Test
    public void shouldSendUnreliableMessageThroughCloud() throws Exception {

        smith.send(Messages.ping(smith, karl));

        Message message = getLastMessageToCloud();
        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(karl.getIden(), message.getTo());
        assertEquals(PIN, message.getType());
        assertEquals(false, message.isReliable());
    }

    @Test
    public void shouldSendReliableMessageThroughCloud() throws Exception {

        smith.send(Messages.ping(smith, karl).reliable());

        Message message = getLastMessageToCloud();
        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(karl.getIden(), message.getTo());
        assertEquals(PIN, message.getType());
        assertEquals(true, message.isReliable());
    }

    @Test
    public void presenceMessageShouldContainNetworkInfo() throws Exception {
        smith.send(Messages.presence(smith, cloud));

        Message message = getLastMessageToCloud();

        assertNotNull(message);
        assertEquals(smith.getIden(), message.getFrom());
        assertEquals(PRS, message.getType());

        assertNotNull(((Presence) message.getData()).getNetworks());
        assertEquals(((Presence) message.getData()).getNetworks(), getNetworks());
    }

    @Test
    public void localAgentLastAccessTimeShouldAlwaysBeNow() throws Exception {
        fakeSystemTime(123456L);

        LocalAgent jeff = new LocalAgent(UUID.randomUUID());
        jeff.join(cloud);

        assertEquals(jeff.getAccessTime(), SystemTime.asMillis());
    }

    @Test
    public void shouldUpdateAccessTimeWhenMessageIsReceived() {
        fakeSystemTime(123456790L);

        Message message = Messages.ping(cloud, smith);
        simulateMessageFromCloud(message);

        assertEquals(123456790L, smith.getAccessTime());
    }

    @Test
    public void otherAgentsShouldNOTStillSeeAgentOnLeave() throws Exception {
        smith.leave();
        assertFalse(karl.getCloud().getLocalAgents().contains(smith));
    }

    private Message getLastMessageToCloud() throws IOException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(cloud, atLeastOnce()).send(captor.capture());
        return captor.getValue();
    }

    private void simulateMessageFromCloud(final Message message) {
        ArgumentCaptor<Cloud.Listener> cloudListener = ArgumentCaptor.forClass(Cloud.Listener.class);
        verify(cloud, atLeastOnce()).addListener(cloudListener.capture());
        cloudListener.getValue().onMessage(message);
    }

    private void fakeSystemTime(final long time) {
        SystemTime.setTimeSource(new SystemTime.TimeSource() {
            public long millis() {
                return time;
            }
        });
    }

    private static Set<Network> getNetworks() throws SocketException {
        Set<Network> nets = new HashSet<Network>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                nets.addAll(Network.list(nic, true));
            }
        } catch (SocketException e) {
            System.out.println("AgentTest.getNetworks" + e);
            throw e;
        }
        return nets;
    }
}
