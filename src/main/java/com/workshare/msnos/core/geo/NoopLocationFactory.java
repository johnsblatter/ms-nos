package com.workshare.msnos.core.geo;

public class NoopLocationFactory implements LocationFactory {

    @Override
    public Location make(String host) {
        return Location.UNKNOWN;
    }

}
