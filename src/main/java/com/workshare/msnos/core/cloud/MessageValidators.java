package com.workshare.msnos.core.cloud;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpiringMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Iden.Type;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.soup.time.SystemTime;

public class MessageValidators {

    private static final String SYSP_MESSAGE_LIFETIME = "msnos.core.message.lifetime";

    public enum Reason {
        TO_LOCAL,       // directed to local agent
        FROM_LOCAL,     // coming from local agent
        TO_OTHER,       // addressed to unknown cloud or agent
        BAD_SIGNED,     // signature is not valid
        DUPLICATE,      // duplicate
        TOO_OLD,        // too old
    }


    private static final Logger log = LoggerFactory.getLogger(MessageValidators.class);

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
    
    public static interface Validator {
        public Result isValid(Message message);
    }

    public static final Result SUCCESS = new Result(true, null);
    
    private final Cloud.Internal cloud;
    private final List<Validator> receivingValidators;
    private final List<Validator> forwardingValidators;

    public MessageValidators(Cloud.Internal aCloud) {
        this.cloud = aCloud;

        final Validator notToLocal = shouldNotDirectedToLocalAgent();
        final Validator notFromLocal = shouldNotComeFromLocalAgent();
//        final Validator neverSeen = shouldNeverSeenMessage();
        final Validator withValidSignature = shouldHaveValidSignature();
        final Validator notTooOld = shouldNotBeTooOld();
        final Validator notAddressedOutside = shouldNotBeAddressedToAnotherCloud();

        this.receivingValidators = Arrays.asList(
                notFromLocal, 
                notAddressedOutside,
                notTooOld,
                shouldNeverSeenMessage(), 
                withValidSignature);

        this.forwardingValidators = Arrays.asList(
                notToLocal, 
                notTooOld,
                shouldNeverSeenMessage(), 
                withValidSignature);
    }

    public Result isReceivable(Message message) {
        return isValid(message, receivingValidators);
    }

    public Result isForwardable(Message message) {
        return isValid(message, forwardingValidators);
    }

    private Result isValid(Message message, final List<Validator> validators) {
        for (Validator validator : validators) {
            final Result result = validator.isValid(message);
            if (!result.success()) {
                log.debug("Message validation failed: {} - message: {}", validator, message);
                return result;
            }
        }
        
        return SUCCESS;
    }

    private long getMessageLifetime() {
        return Long.getLong(SYSP_MESSAGE_LIFETIME, 60000);
    }

    private Validator shouldHaveValidSignature() {
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

    private Validator shouldNeverSeenMessage() {
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

    private Validator shouldNotBeTooOld() {
        return new AbstractMessageValidator(Reason.TOO_OLD) {
            
            private final long lifetime = getMessageLifetime();

            @Override
            public Result isValid(Message message) {
                final long now = SystemTime.asMillis();
                final long elapsed = now - message.getWhen();
                final Result result = asResult(elapsed < lifetime);
                return result;
            }
        };
    }

    private Validator shouldNotBeAddressedToAnotherCloud() {
        return new AbstractMessageValidator(Reason.TO_OTHER) {
            @Override
            public Result isValid(Message message) {
                final Iden to = message.getTo();
                boolean success = to.getType() != Type.CLD || to.equals(cloud.cloud().getIden());
                return asResult(success);
            }};
    }

    private Validator shouldNotComeFromLocalAgent() {
        return new AbstractMessageValidator(Reason.FROM_LOCAL) {
            @Override
            public Result isValid(Message message) {
                return asResult(!cloud.localAgents().containsKey(message.getFrom()));
            }};
    }

    private Validator shouldNotDirectedToLocalAgent() {
        return new AbstractMessageValidator(Reason.TO_LOCAL) {
            @Override
            public Result isValid(Message message) {
                return asResult(!cloud.localAgents().containsKey(message.getTo()));
            }};
    }

    abstract class AbstractMessageValidator implements Validator {
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
