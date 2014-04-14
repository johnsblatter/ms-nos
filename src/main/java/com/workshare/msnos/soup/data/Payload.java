package com.workshare.msnos.soup.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Payload {

    private static final Logger log = LoggerFactory.getLogger(Payload.class);

    private boolean presence;

    public void setPresence(boolean is) {
        presence = is;
    }

    public boolean isPresence() {
        return presence;
    }
}
