package com.workshare.msnos.core.payloads;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Cloud.Internal;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Payload;
import com.workshare.msnos.core.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
                cloud.send(new MessageBuilder(Message.Type.DSC, cloud, message.getFrom()).make());
            } catch (IOException e) {
                log.error("Unexpected exception sending message " + message, e);
            }

        return true;
    }

}
