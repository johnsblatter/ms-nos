package com.workshare.msnos.core;

import java.util.Set;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.soup.threading.ExecutorServices;

public class Sender {

    public static final String SYSP_SENDER_THREADS_NUM = "com.ws.msnos.sender.threads.num";

    public class Transmission implements Runnable {
        private Cloud cloud;
        private Message message;
        private MultiReceipt receipt;

        public Transmission(Message message, Cloud cloud) {
            super();
            this.message = message;
            this.cloud = cloud;
            this.receipt = new MultiReceipt(message);
        }

        public Cloud cloud() {
            return cloud;
        }

        public Message message() {
            return message;
        }

        public MultiReceipt receipt() {
            return receipt;
        }

        @Override
        public void run() {
            sendSync(this.cloud(), this.message(), this.receipt());
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Sender.class);
    private final Executor executor;

    
    Sender() {
        this(ExecutorServices.newFixedDaemonThreadPool(getThreadNum()));  
    }
    
    Sender(Executor executor) {
        this.executor = executor;
    }

    public Receipt send(final Cloud acloud, final Message amessage) throws MsnosException {
        Transmission tx = new Transmission(amessage, acloud);
        executor.execute(tx);
        return tx.receipt();
    }

    void sendSync(final Cloud cloud, final Message message, final MultiReceipt multi) {
        final Set<Gateway> allGates = cloud.getGateways();
        for (Gateway gate : allGates) {
            try {
                log.debug("Sending {} message {} to {} via gate {}", message.getType(), message.getUuid(), message.getTo(), gate.name());
                final Receipt receipt = gate.send(cloud, message);
                multi.add(receipt);
                log.debug("Status: {}", receipt.getStatus());
                if (receipt.getStatus() == Message.Status.DELIVERED)
                    break;
            } catch (Throwable ex) {
                log.warn("Unable to send message " + message + " trough gateway " + gate, ex);
            }
        }

        if (multi.size() == 0) {
            multi.add(SingleReceipt.failure(message));
        }
    }

    private static Integer getThreadNum() {
        return Integer.getInteger(SYSP_SENDER_THREADS_NUM, 3);
    }


}
