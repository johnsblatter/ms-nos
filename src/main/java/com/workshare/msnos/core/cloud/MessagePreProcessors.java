package com.workshare.msnos.core.cloud;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.RemoteEntity;

public class MessagePreProcessors {

    private static final Logger log = LoggerFactory.getLogger(MessagePreProcessors.class);

    public static interface MessagePreProcessor {
        public boolean isValid(Message message)
        ;
    }

    private final Cloud.Internal cloud;
    private final List<MessagePreProcessor> validators;

    public MessagePreProcessors(Cloud.Internal aCloud) {
        this.cloud = aCloud;

        this.validators = new ArrayList<MessagePreProcessor>();
        validators.add(shouldNotComeFromLocalAgent());
        validators.add(shouldNotBeAddressedToRemoteAgent());
        validators.add(shouldNotBeAddressedToAnotherCloud());
        validators.add(shouldBeInSequence());
        validators.add(shouldHaveValidSignature());
    }

    public boolean isValid(Message message) {
        for (MessagePreProcessor validator : validators) {
            if (!validator.isValid(message)) {
                log.debug("Message validation failed: {} - message: {}", validator, message);
                return false;
            }
        }
        
        return true;
    }

    private MessagePreProcessor shouldHaveValidSignature() {
        return new AbstractMessageValidator("signature is not valid") {
            @Override
            public boolean isValid(Message message) {
                Message signed = cloud.sign(message);
                return parseNull(message.getSig()).equals(parseNull(signed.getSig()));
            }
            
            private String parseNull(String signature) {
                return (signature == null ? "" : signature);
            }};
    }

    private MessagePreProcessor shouldBeInSequence() {
        return new AbstractMessageValidator("out of sequence") {
            @Override
            public boolean isValid(Message message) {
                RemoteEntity remote = getRemote(message);
                if (remote == null) 
                    return true;
                else    
                    return remote.accept(message);
            }

            private RemoteEntity getRemote(Message message) {
                if (message.getFrom().getType() == Iden.Type.CLD)
                    return getRemoteCloud(message);
                else
                    return cloud.remoteAgents().get(message.getFrom());
            }

            private RemoteEntity getRemoteCloud(final Message message) {
                final Iden from = message.getFrom();
                if (from.getSuid() == null)
                    return null;
                        
                RemoteEntity remoteCloud;
                if (!cloud.remoteClouds().containsKey(from)) {
                    remoteCloud = new RemoteEntity(from, cloud.cloud());
                    remoteCloud.accept(message);
                    cloud.remoteClouds().add(remoteCloud);
                } else {
                    remoteCloud = cloud.remoteClouds().get(from);
                }
                
                return remoteCloud;
            }
        };
    }

    private MessagePreProcessor shouldNotBeAddressedToAnotherCloud() {
        return new AbstractMessageValidator("addressed to another cloud") {
            @Override
            public boolean isValid(Message message) {
                final Iden to = message.getTo();
                return to.equals(cloud.cloud().getIden()) || cloud.localAgents().containsKey(to);        
            }};
    }

    private MessagePreProcessor shouldNotBeAddressedToRemoteAgent() {
        return new AbstractMessageValidator("addressed to a remote agent") {
            @Override
            public boolean isValid(Message message) {
                return !cloud.remoteAgents().containsKey(message.getTo());
            }};
    }

    private MessagePreProcessor shouldNotComeFromLocalAgent() {
        return new AbstractMessageValidator("coming from local agent") {
            @Override
            public boolean isValid(Message message) {
                return !cloud.localAgents().containsKey(message.getFrom());
            }};
    }

    abstract class AbstractMessageValidator implements MessagePreProcessor {
        private String message;

        AbstractMessageValidator(String message) {
            this.message = message;
        }
        
        @Override
        public final String toString() {
            return message;
        }
        
    }
}
