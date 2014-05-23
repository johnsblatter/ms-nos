package com.workshare.msnos.core;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.protocols.ip.Endpoint;

public class NoopGateway implements Gateway {

	@Override
	public void addListener(Cloud cloud, Listener listener) {
	}

	@Override
	public Set<? extends Endpoint> endpoints() {
		return Collections.emptySet();
	}

	@Override
	public Receipt send(Cloud cloud, Message message) throws IOException {
		return new SingleReceipt(Status.UNKNOWN, message);
	}

    @Override
    public void close() throws IOException {
    }

}
