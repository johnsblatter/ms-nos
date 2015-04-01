package com.workshare.msnos.core.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpiringMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.soup.time.SystemTime;

public class MessagePreProcessors {

    private static final String SYSP_MESSAGE_LIFETIME = "msnos.core.message.lifetime";

    public enum Reason {
        FROM_LOCAL,     // coming from local agent
        FOR_REMOTE,     // addressed to a remote agent
        UNKNOWN_TO,     // addressed to unknown cloud or agent
        BAD_SIGNED,     // signature is not valid
        DUPLICATE,      // duplicate
        TOO_OLD,        // too old
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
        validators.add(shouldNeverSeenMessage());
        validators.add(shouldNotBeTooOld());
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

    private long getMessageLifetime() {
        return Long.getLong(SYSP_MESSAGE_LIFETIME, 60);
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

    private MessagePreProcessor shouldNeverSeenMessage() {
        return new AbstractMessageValidator(Reason.DUPLICATE) {
            
            private final ExpiringMap<UUID, Message> messages = ExpiringMap.builder()
                        .expiration(getMessageLifetime(), TimeUnit.SECONDS)
                        .build();            

            @Override
            public Result isValid(Message message) {
                if (messages.containsKey(message.getUuid()))
                    return failure;
                
                messages.put(message.getUuid(), message);
                return SUCCESS;
            }
        };
    }

    private MessagePreProcessor shouldNotBeTooOld() {
        return new AbstractMessageValidator(Reason.TOO_OLD) {
            
            private final long lifetime = getMessageLifetime();

            @Override
            public Result isValid(Message message) {
                final long elapsed = System.currentTimeMillis() - message.getWhen();
                return asResult(elapsed < lifetime);
            }
        };
    }

    private MessagePreProcessor shouldNotBeAddressedToAnotherCloud() {
        return new AbstractMessageValidator(Reason.UNKNOWN_TO) {
            @Override
            public Result isValid(Message message) {
                final Iden to = message.getTo();
                boolean success = to.equals(cloud.cloud().getIden()) || cloud.localAgents().containsKey(to);
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
