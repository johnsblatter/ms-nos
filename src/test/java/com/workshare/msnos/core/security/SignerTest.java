package com.workshare.msnos.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.Message.Type;
import com.workshare.msnos.core.MessageBuilder;
import com.workshare.msnos.core.serializers.WireSerializer;

public class SignerTest {

    private static final String MESSAGE_AS_TEXT = "This is the serialized version of the message :)";
    private static final String KEY_ID = "123";
    private static final String KEY_VAL = "1234567890";
    
    private KeysStore keys;
    private WireSerializer serializer;
    private Signer signer;
    private Message message;

    @Before
    public void init() {
        keys = mock(KeysStore.class);
        when(keys.get(KEY_ID)).thenReturn(KEY_VAL);
        serializer = mock(WireSerializer.class);
        when(serializer.toText(anyObject())).thenReturn(MESSAGE_AS_TEXT);
        
        final Iden src = new Iden(Iden.Type.CLD, UUID.randomUUID());
        final Iden dst = new Iden(Iden.Type.AGT, UUID.randomUUID());
        message = new MessageBuilder(Type.PIN, src, dst).make();        

        signer = new Signer(serializer, keys);
    }
    
    
    @Test
    public void shouldUsAccessTheKeystore() throws Exception {        
        signer.signed(message, KEY_ID);
        verify(keys).get(KEY_ID);        
    }
    
    @Test
    public void shouldSignUsingTheStoreKey() throws Exception {        
        Message result = signer.signed(message, KEY_ID);
        String signature = KEY_ID+":"+sign(KEY_VAL, MESSAGE_AS_TEXT);
        assertEquals(signature, result.getSig());
    }
    
    @Test
    public void shouldLeaveMessagUnsignedIfKeystoreEmpty() throws Exception {        
        when(keys.get(anyString())).thenReturn(null);
        Message result = signer.signed(message, KEY_ID);
        assertNull(result.getSig());
    }
    
    
    private String sign(String key, String text) throws Exception {
        byte[] keyBytes = key.getBytes("UTF-8");
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        
        byte[] textBytes = mac.doFinal(text.getBytes("UTF-8"));
        return DatatypeConverter.printHexBinary(textBytes);
    }

}
