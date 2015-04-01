package com.workshare.msnos.core;

import static com.workshare.msnos.soup.Shorteners.shorten;

import java.util.Set;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.ExecutorServices;

public class Sender {

    public static final String SYSP_SENDER_THREADS_NUM = "com.ws.msnos.sender.threads.num";

    private static final Logger proto = LoggerFactory.getLogger("protocol");

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
        if (amessage.getHops() == 0) {
            logNX(amessage, "ANY");
            return SingleReceipt.failure(amessage);
        }
            
        Transmission tx = new Transmission(amessage.hopped(), acloud);
        executor.execute(tx);
        return tx.receipt();
    }

    void sendSync(final Cloud cloud, final Message message, final MultiReceipt multi) {
        String gateName = null;
        final Set<Gateway> allGates = cloud.getGateways();
        for (Gateway gate : allGates) {
            try {
                log.debug("Sending {} message {} to {} via gate {}", message.getType(), message.getUuid(), message.getTo(), gate.name());
                final Receipt receipt = gate.send(cloud, message);
                multi.add(receipt);
                log.debug("Status: {}", receipt.getStatus());
                if (receipt.getStatus() == Message.Status.DELIVERED) {
                    gateName = gate.name();
                    break;
                }
            } catch (Throwable ex) {
                log.warn("Unable to send message " + message + " trough gateway " + gate, ex);
            }
        }

        if (multi.size() == 0) {
            multi.add(SingleReceipt.failure(message));
            logTX(message, "FAIL");
        }
        else
            logTX(message, (gateName == null ? multi.getGate() : gateName));
    }

    private static Integer getThreadNum() {
        return Integer.getInteger(SYSP_SENDER_THREADS_NUM, 3);
    }

    private void logTX(Message msg, String gateName) {
        if (!proto.isInfoEnabled())
            return;

        final String muid = shorten(msg.getUuid());
        final String payload = Json.toJsonString(msg.getData());
        final String when = shorten(msg.getWhen());
        proto.info("TX({}): {} {} {} {} {} {}", gateName, msg.getType(), muid, when, msg.getFrom(), msg.getTo(), payload);
    }

    private void logNX(Message msg, String gateName) {
        if (!proto.isDebugEnabled())
            return;

        final String muid = shorten(msg.getUuid());
        final String payload = Json.toJsonString(msg.getData());
        final String when = shorten(msg.getWhen());
        proto.debug("NX({}): {} {} {} {} {} {}", gateName, msg.getType(), muid, when, msg.getFrom(), msg.getTo(), payload);
    }

}
