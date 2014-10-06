package com.workshare.msnos.core;

import com.workshare.msnos.soup.time.SystemTime;

import java.util.concurrent.atomic.AtomicLong;

public class RemoteEntity implements Identifiable {

    private final Iden iden;
    private final Cloud cloud;
    private final AtomicLong sequencer;
    private long accessTime;

    public RemoteEntity(Iden iden, Cloud cloud) {
        this.iden = iden;
        this.cloud = cloud;
        this.sequencer = new AtomicLong();
    }

    public Iden getIden() {
        return iden;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public Long getNextSequence() {
        return sequencer.get();
    }

    public boolean accept(Message message) {
        long seq = message.getSequence();
        if (seq < sequencer.get())
            return false;

        sequencer.set(seq);
        return true;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void touch() {
        this.accessTime = SystemTime.asMillis();
    }
}
