package com.workshare.msnos.core.protocols.ip;

import java.io.IOException;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Gateway;
import com.workshare.msnos.core.Identifiable;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Status;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.receipts.SingleReceipt;

public class NullGateway implements Gateway {

    public final static String NAME = "NOOP";

    @Override
    public String name() {
        return NAME;
    }

    @Override
	public void addListener(Cloud cloud, Listener listener) {
	}

	@Override
	public Endpoints endpoints() {
		return BaseEndpoint.create();
	}

	@Override
	public Receipt send(Cloud cloud, Message message, Identifiable to) throws IOException {
		return new SingleReceipt(this, Status.FAILED, message);
	}

    @Override
    public void close() throws IOException {
    }

}
