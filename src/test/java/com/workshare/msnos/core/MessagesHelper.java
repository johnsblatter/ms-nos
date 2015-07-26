package com.workshare.msnos.core;

import static com.workshare.msnos.core.Message.Type.HCK;

import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.HealthcheckPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.core.services.api.RemoteMicroservice;
import com.workshare.msnos.core.services.api.RestApi;

public class MessagesHelper {

    private MessagesHelper() {}

    public static Message newHCKMessage(RemoteMicroservice remote, boolean working) {
        final RemoteAgent agent = remote.getAgent();
        final Cloud cloud = agent.getCloud();
        return new MessageBuilder(HCK, cloud, cloud)
                .with(new HealthcheckPayload(agent, working))
                .make();
    }
    
    public static Message newPingMessage(Iden from) {
        return new MessageBuilder(Message.Type.PIN, from, from).make();
    }

    public static Message newPingMessage(Identifiable from) {
        return newPingMessage(from, from);
    }

    public static Message newPingMessage(Identifiable from, Identifiable to) {
        return new MessageBuilder(Message.Type.PIN, from.getIden(), to.getIden()).make();
    }

    public static Message newPongMessage(Identifiable from, Identifiable to) {
        return new MessageBuilder(Message.Type.PON, from.getIden(), to.getIden()).make();
    }

    public static Message newAPPMessage(RemoteMicroservice remote, Identifiable to) {
        RemoteAgent from = remote.getAgent();
        return newAPPMesage(from, to);
    }

    public static Message newAPPMesage(Agent from, Identifiable to) {
        return new MessageBuilder(Message.Type.APP, from, to).make();
    }

    public static Message newQNEMessage(RemoteEntity from, String name, RestApi... apis) {
        final Cloud cloud = from.getCloud();
        return new MessageBuilder(Message.Type.QNE, from.getIden(), cloud.getIden()).with(new QnePayload(name, apis)).make();
    }

    public static Message newFaultMessage(Agent agent) {
        Cloud cloud = agent.getCloud();
        return new MessageBuilder(Message.Type.FLT, cloud, cloud).with(new FltPayload(agent.getIden())).make();
    }

    public static Message newPresenceMessage(Agent from, final boolean present) throws MsnosException {
        Cloud cloud = from.getCloud();
        return new MessageBuilder(Message.Type.PRS, from, cloud).with(new Presence(present, from)).make();
    }


}
