package com.workshare.msnos.core.cloud;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workshare.msnos.core.LocalAgent;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.MsnosException;

public class JoinSynchronizer  {
    private static Logger log = LoggerFactory.getLogger(JoinSynchronizer.class);

    private final List<Status> joiners = new CopyOnWriteArrayList<Status>();

    public Status start(LocalAgent agent) {
        final Status status = new Status(agent);
        joiners.add(status);
        return status;
    }

    public void wait(Status status) throws MsnosException {
        status.sync();
    }
    
    public void process(Message message) {
        log.debug("updatejoiners: "+message);
        for (Status joiner : joiners) {
            joiner.process(message);
        }
    }

    public void remove(Status status) {
        joiners.remove(status);
    }

    
    public static class Status {

        private final CountDownLatch latch;
        private final LocalAgent agent;
        
        private Status(LocalAgent agent) {
            this.agent = agent;
            this.latch = new CountDownLatch(1);
        }
        
        public void process(Message message) {
            if (message.getType() != Type.PRS)
                return;
            
            if (message.getFrom().equals(agent.getIden())) {
                latch.countDown();
            }
        }
        
        public void sync() throws MsnosException {
            try {
                latch.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            
            if (latch.getCount() > 0)
                throw new MsnosException("Unsuccessful cloud join for agent "+agent, MsnosException.Code.JOIN_FAILED);
            
            log.debug("Join synchronisation successful");
        }
    }
}
