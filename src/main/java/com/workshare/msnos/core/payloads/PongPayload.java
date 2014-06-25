package com.workshare.msnos.core.payloads;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.Message.Payload;

public class PongPayload implements Message.Payload {

    private static Logger log = LoggerFactory.getLogger(PongPayload.class);

    @Override
    public Payload[] split() {
        return new Payload[]{this};
    }

    @Override
    public boolean process(Message message, Internal internal) {
        if (!internal.remoteAgents().containsKey(message.getFrom()))
            try {
                final Cloud cloud = internal.cloud();
                cloud.send(new MessageBuilder(Message.Type.DSC, cloud.getIden(), message.getFrom()).make());
            } catch (IOException e) {
                log.error("Unexpected exception sending message " + message, e);
            }
        
        return true;
   }

}
