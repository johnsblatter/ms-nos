package com.workshare.msnos.core.payloads;

import static com.workshare.msnos.core.CoreHelper.createMockCloud;
import static com.workshare.msnos.core.CoreHelper.getCloudInternal;
import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static com.workshare.msnos.core.CoreHelper.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.MsnosException;
import com.workshare.msnos.core.cloud.IdentifiablesList;
import com.workshare.msnos.core.payloads.TracePayload.Crumb;
import com.workshare.msnos.core.protocols.ip.NullGateway;

public class TracePayloadTest {

    private Cloud cloud;
    private Internal internal;
    private LocalAgent local;

    @Before
    public void setup() {
        cloud = createMockCloud();
        internal = getCloudInternal(cloud);

        local = new LocalAgent(randomUUID());
        IdentifiablesList<LocalAgent> locals = internal.localAgents();
        locals.add(local);
    }

    @Test
    public void shouldSplitCrumbsCollectionOnSplit() {
        final Crumb alfa = mock(Crumb.class);
        final Crumb beta = mock(Crumb.class);
        TracePayload payload = new TracePayload(newAgentIden(), Arrays.asList(alfa,beta));

        Payload[] loads = payload.split();
        assertEquals(2, loads.length);

        List<Crumb> crumbs = new ArrayList<Crumb>();
        crumbs.addAll(((TracePayload)loads[0]).crumbs());
        crumbs.addAll(((TracePayload)loads[1]).crumbs());
        assertEquals(payload.crumbs(), crumbs);
    }

    @Test
    public void shouldStoreCrumbs() {
        TracePayload payload = new TracePayload(newAgentIden(), new ArrayList<Crumb>());
        UUID src = UUID.randomUUID();
        UUID dst = UUID.randomUUID();
        
        payload = payload.crumbed(src, dst, new NullGateway(), 3);

        List<Crumb> crumbs = payload.crumbs();
        assertEquals(1, crumbs.size());
        Crumb crumb = crumbs.get(0);
        assertEquals(src, crumb.source());
        assertEquals(dst, crumb.destination());
        assertEquals(3, crumb.hops());
        assertEquals(NullGateway.NAME, crumb.way());
    }

    @Test
    public void shouldProcessDoNothingIfMessageNotDestinedToLocal() throws Exception {
        
        TracePayload payload = createCrumbedPayload(randomUUID(), randomUUID());
        Message message = new MessageBuilder(Type.TRC, newAgentIden(), newAgentIden()).with(payload).make();

        payload.process(message, internal);
        
        verify(cloud, never()).send(any(Message.class));
    }


    @Test
    public void shouldProcessSendACKBackWhenMessageDestinedToLocal() throws Exception {
        TracePayload payload = createCrumbedPayload(randomUUID(), randomUUID());
        Message trace = new MessageBuilder(Type.TRC, newAgentIden(), local.getIden()).with(payload).make();

        payload.process(trace, internal);

        final Message messsage = getLastMessageSent();
        assertEquals(Message.Type.ACK, messsage.getType());
        assertEquals(local.getIden(), messsage.getFrom());
        assertEquals(payload.from(), messsage.getTo());
        assertEquals(payload, messsage.getData());
    }

    private Message getLastMessageSent() throws MsnosException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(cloud).send(captor.capture());
        final Message sent = captor.getValue();
        assertNotNull(sent);
        return sent;
    }

    private TracePayload createCrumbedPayload(UUID src, UUID dst) {
        TracePayload payload = new TracePayload(newAgentIden(), new ArrayList<Crumb>());
        payload = payload.crumbed(src, dst, new NullGateway(), 3);
        return payload;
    }

}
