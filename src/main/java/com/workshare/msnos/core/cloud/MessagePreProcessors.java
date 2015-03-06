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

    public enum Reason {
        FROM_LOCAL,     // coming from local agent
        FOR_REMOTE,     // addressed to a remote agent
        UNKNOWN_TO,     // addressed to unknown cloud or agent
        BAD_SIGNED,     // signature is not valid
        OUT_OF_SEQ,     // out of sequence
        TOO_OLD,     // out of sequence
    }


    private static final Logger log = LoggerFactory.getLogger(MessagePreProcessors.class);

    public static class Result {
        private final boolean success;
        private final Reason reason;

        public Result(boolean success, Reason reason) {
            super();
            this.success = success;
            this.reason = reason;
        }

        public boolean success() {
            return success;
        }

        public String reason() {
            return reason.toString();
        }
    }
    
    public static interface MessagePreProcessor {
        public Result isValid(Message message);
    }

    private static final Result SUCCESS = new Result(true, null);
    
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

    public Result isValid(Message message) {
        for (MessagePreProcessor validator : validators) {
            final Result result = validator.isValid(message);
            if (!result.success()) {
                log.debug("Message validation failed: {} - message: {}", validator, message);
                return result;
            }
        }
        
        return SUCCESS;
    }

    private MessagePreProcessor shouldHaveValidSignature() {
        return new AbstractMessageValidator(Reason.BAD_SIGNED) {
            @Override
            public Result isValid(Message message) {
                Message signed = cloud.sign(message);
                return asResult(parseNull(message.getSig()).equals(parseNull(signed.getSig())));
            }
            
            private String parseNull(String signature) {
                return (signature == null ? "" : signature);
            }};
    }

    private MessagePreProcessor shouldBeInSequence() {
        return new AbstractMessageValidator(Reason.OUT_OF_SEQ) {
            @Override
            public Result isValid(Message message) {
                RemoteEntity remote = getRemote(message);
                if (remote == null) 
                    return SUCCESS;
                else    
                    return asResult(remote.accept(message));
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
        return new AbstractMessageValidator(Reason.UNKNOWN_TO) {
            @Override
            public Result isValid(Message message) {
                final Iden to = message.getTo();
                boolean success = to.equalsAsIdens(cloud.cloud().getIden()) || cloud.localAgents().containsKey(to);
                return asResult(success);
            }};
    }

    private MessagePreProcessor shouldNotBeAddressedToRemoteAgent() {
        return new AbstractMessageValidator(Reason.FOR_REMOTE) {
            @Override
            public Result isValid(Message message) {
                return asResult(!cloud.remoteAgents().containsKey(message.getTo()));
            }};
    }

    private MessagePreProcessor shouldNotComeFromLocalAgent() {
        return new AbstractMessageValidator(Reason.FROM_LOCAL) {
            @Override
            public Result isValid(Message message) {
                return asResult(!cloud.localAgents().containsKey(message.getFrom()));
            }};
    }

    abstract class AbstractMessageValidator implements MessagePreProcessor {
        protected final Reason reason;
        protected final Result failure;

        AbstractMessageValidator(Reason message) {
            this.reason = message;
            this.failure = new Result(false, message);
        }
        
        public Result asResult(boolean success) {
            return success ? SUCCESS : failure;
        }
        
        @Override
        public final String toString() {
            return reason.toString();
        }
        
    }
}
