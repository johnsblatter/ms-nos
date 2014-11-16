package com.workshare.msnos.core;

import static com.workshare.msnos.core.Message.Type.HCK;

import com.workshare.msnos.core.payloads.FltPayload;
import com.workshare.msnos.core.payloads.HealthcheckPayload;
import com.workshare.msnos.core.payloads.Presence;
import com.workshare.msnos.core.payloads.QnePayload;
import com.workshare.msnos.usvc.RemoteMicroservice;
import com.workshare.msnos.usvc.api.RestApi;

public class MessagesHelper {

    private MessagesHelper() {}

    public static Message newHCKMessage(RemoteMicroservice remote, boolean working) {
        final RemoteAgent agent = remote.getAgent();
        final Cloud cloud = agent.getCloud();
        return new MessageBuilder(HCK, cloud, cloud)
                .with(new HealthcheckPayload(agent, working))
                .make();
    }
    
    public static Message newAPPMessage(RemoteMicroservice remote, Identifiable to) {
        RemoteAgent from = remote.getAgent();
        return new MessageBuilder(Message.Type.APP, from, to).make();
    }

    public static Message newQNEMessage(RemoteEntity from, String name, RestApi... apis) {
        final Cloud cloud = from.getCloud();
        return new MessageBuilder(MessageBuilder.Mode.RELAXED, Message.Type.QNE, from.getIden(), cloud.getIden()).with(new QnePayload(name, apis)).make();
    }

    public static Message newFaultMessage(Agent agent) {
        Cloud cloud = agent.getCloud();
        return new MessageBuilder(Message.Type.FLT, cloud, cloud).with(new FltPayload(agent.getIden())).make();
    }

    public static Message newLeaveMessage(RemoteAgent from) throws MsnosException {
        Cloud cloud = from.getCloud();
        return new MessageBuilder(Message.Type.PRS, from, cloud).with(new Presence(false)).make();
    }


}
