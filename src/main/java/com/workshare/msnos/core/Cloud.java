package com.workshare.msnos.core;

import java.util.UUID;

import com.workshare.msnos.soup.json.Json;

public class Cloud {

    private Iden iden;

    public Cloud(UUID uuid) {
        this.iden = new Iden(Iden.Type.CLD, uuid);
    }

    public Iden getIden() {
        return iden;
    }
    
    public String toString() {
        return Json.toJsonString(this);
    }
}
