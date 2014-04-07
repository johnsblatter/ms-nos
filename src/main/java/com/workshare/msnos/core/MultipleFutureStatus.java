package com.workshare.msnos.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.workshare.msnos.core.Message.Status;

public class MultipleFutureStatus implements CompositeFutureStatus {

	private Set<Future<Status>> futures = new HashSet<Future<Status>>();
	private volatile boolean done = false;
	
    @Override
	public boolean cancel(boolean arg0) {
	    return false;
	}

	@Override
	public Status get() throws InterruptedException, ExecutionException {
	    while(true) {
    	    try {
                return get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                continue;
            }
	    }
    }

	@Override
	public Status get(long num, TimeUnit unit)
			throws InterruptedException, ExecutionException,
			TimeoutException {
        Status res = Status.UNKNOWN;
        for (Future<Status> future : futures) {
            Status status = future.get(num, unit);
            if (status == Status.PENDING) {
                res = status;
            }
            else if (status == Status.DELIVERED) {
                res = status;
                done = true;
                break;
            }
        }
        
        return res;
	}

    @Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
    public void add(Future<Status> status) {
	    futures.add(status);
    }
	
	Set<Future<Status>> statuses() {
        return futures ;
	}
}
