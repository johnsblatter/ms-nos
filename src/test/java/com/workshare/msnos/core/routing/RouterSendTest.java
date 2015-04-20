package com.workshare.msnos.core.routing;

import java.io.IOException;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;

public class RouterSendTest extends RouterAbstractTest {

    protected Receipt process(Message message) throws IOException {
        return router().send(message);
    }
}
