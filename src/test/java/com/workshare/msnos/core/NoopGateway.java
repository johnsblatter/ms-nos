package com.workshare.msnos.core;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Future;

import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.protocols.ip.Endpoint;

public class NoopGateway implements Gateway {

	@Override
	public void addListener(Listener listener) {
	}

	@Override
	public Set<? extends Endpoint> endpoints() {
		return Collections.emptySet();
	}

	@Override
	public Future<Status> send(Message message) throws IOException {
		return new UnknownFutureStatus();
	}

}
