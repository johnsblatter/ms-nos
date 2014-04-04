package com.workshare.msnos.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Cloud {

    private static final Logger log = LoggerFactory.getLogger(Cloud.class);

    private Iden iden;

    public Cloud(UUID uuid) {
        this.iden = new Iden(Iden.Type.CLD, uuid);
    }

    public Iden getIden() {
        return iden;
    }
}
