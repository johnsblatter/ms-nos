package com.workshare.msnos.core;

import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.soup.time.SystemTime;

import java.util.concurrent.atomic.AtomicLong;

public class RemoteEntity implements Identifiable {

    private final Iden iden;
    private final AtomicLong accessTime;
    transient private final Cloud cloud;

    public RemoteEntity(Iden iden, Cloud cloud) {
        this.iden = iden;
        this.cloud = cloud;
        this.accessTime = new AtomicLong();
    }

    public Iden getIden() {
        return iden;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public long getAccessTime() {
        return accessTime.longValue();
    }

    public void touch() {
        this.accessTime.set(SystemTime.asMillis());
    }
}
