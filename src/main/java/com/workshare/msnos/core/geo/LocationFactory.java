package com.workshare.msnos.core.geo;

public interface LocationFactory {

    public abstract Location make(String host);

    public static final LocationFactory DEFAULT = OfflineLocationFactory.build();
}