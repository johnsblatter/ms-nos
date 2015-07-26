package com.workshare.msnos.core.routing;

import static com.workshare.msnos.core.cloud.CoreHelper.newAPPMesage;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Test;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Receipt;
import com.workshare.msnos.core.cloud.MessageValidators.Reason;
import com.workshare.msnos.core.cloud.MessageValidators.Result;
public class RouterForwardTest extends RouterAbstractTest {

    protected Receipt process(Message message) throws IOException {
        return router().forward(message);
    }

    @Test
    public void shouldNotForwardMessagesWhenValidatorDenies() throws Exception {
        Message message = newAPPMesage(usaOne, self).make().withHops(10);
        when(validators.isForwardable(message)).thenReturn(new Result(false, Reason.TOO_OLD));

        process(message);

        verifyZeroInteractions(udp, http, www);
    }
}
