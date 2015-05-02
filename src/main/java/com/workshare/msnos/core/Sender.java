package com.workshare.msnos.core;

import static com.workshare.msnos.soup.Shorteners.shorten;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.receipts.SingleReceipt;
import com.workshare.msnos.core.routing.Router;
import com.workshare.msnos.soup.json.Json;
import com.workshare.msnos.soup.threading.ExecutorServices;

public class Sender {

    private static final ExecutorService DEFAULT_EXECUTOR_SERVICE = ExecutorServices.newFixedDaemonThreadPool(getThreadNum());

    public static final String SYSP_SENDER_THREADS_NUM = "com.ws.msnos.sender.threads.num";
    
    private static final Logger log = LoggerFactory.getLogger(Sender.class);
    private static final Logger proto = LoggerFactory.getLogger("protocol");

    public class Transmission implements Runnable {
        private Cloud cloud;
        private Message message;
        private SingleReceipt receipt;

        public Transmission(Message message, Cloud cloud) {
            super();
            this.message = message;
            this.cloud = cloud;
            this.receipt = SingleReceipt.unknown(message);
        }

        public Cloud cloud() {
            return cloud;
        }

        public Message message() {
            return message;
        }

        public SingleReceipt receipt() {
            return receipt;
        }

        @Override
        public void run() {
            sendSync(this.cloud(), this.message(), this.receipt());
        }
    }

    private final Executor executor;
    private final Router router;

    Sender(Router router) {
        this(router, DEFAULT_EXECUTOR_SERVICE);
    }

    Sender(Router router, Executor executor) {
        this.router = router;
        this.executor = executor;
    }

    public Receipt send(final Cloud cloud, final Message amessage) throws MsnosException {  
        log.debug("Accepted message for delivery {} on cloud {}", amessage, cloud);
        Transmission tx = new Transmission(amessage.hopped(), cloud);
        executor.execute(tx);
        return tx.receipt();
    }

    void sendSync(final Cloud cloud, final Message message, final SingleReceipt receipt) {
        Receipt current = router.send(message);
        receipt.update(current);
        log.debug("Message {} routed, receipt {}", message, receipt);

        logTX(message, receipt.getGate());
    }

    private static Integer getThreadNum() {
        return Integer.getInteger(SYSP_SENDER_THREADS_NUM, 3);
    }

    private void logTX(Message msg, String gateName) {
        if (!proto.isInfoEnabled())
            return;

        final String muid = shorten(msg.getUuid());
        final String payload = Json.toJsonString(msg.getData());
        proto.info("TX({}): {} {} {} {} {} {}", gateName, msg.getType(), muid, msg.getWhen(), msg.getFrom(), msg.getTo(), payload);
    }
}
