package com.workshare.msnos.core.security;

import java.io.IOException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.serializers.WireJsonSerializer;
import com.workshare.msnos.core.serializers.WireSerializer;

public class Signer {

    private final WireSerializer sz;
    private final KeysStore keys;

    public Signer() {
        this(new SystemPropertiesKeysStore());
    }
    
    public Signer(KeysStore keys) {
        this(new WireJsonSerializer(), keys);
    }
    
    public Signer(WireSerializer sz, KeysStore keys) {
        this.sz = sz;
        this.keys = keys;
    }
    
    public Message signed(Message message, String keyId) throws IOException {
        String key = keys.get(keyId);
        return message.signed(keyId, signText(key, sz.toText(message)));
    }

    private String signText(String key, String text) throws IOException {
        byte[] keyBytes = key.getBytes("UTF-8");
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] textBytes = mac.doFinal(text.getBytes("UTF-8"));
            return DatatypeConverter.printHexBinary(textBytes);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
