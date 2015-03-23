package com.workshare.msnos.core.geo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.maxmind.geoip2.DatabaseReader;
import com.workshare.msnos.core.protocols.ip.Network;

public class OfflineLocationFactory implements LocationFactory {

    private static final String DB_FILENAME = "geolite2-city.mmdb";
    private DatabaseReader database;

    OfflineLocationFactory() throws IOException {
        this.database = new DatabaseReader.Builder(OfflineLocationFactory.class.getResourceAsStream("/"+DB_FILENAME)).build();
    }

    public OfflineLocationFactory(DatabaseReader database) {
        this.database = database;
    }

    @Override
    public Location make(String host) {
        try {
            return new Location(database.omni(InetAddress.getByName(asValidatedAddress(host))));
        }
        catch (Throwable ignore) {
            return Location.UNKNOWN;
        }
    }

    public DatabaseReader database() {
        return database;
    }

    private String asValidatedAddress(String host) throws UnknownHostException {
        if (Network.isValidDottedIpv4Address(host))
            return host;
        else  {
            final InetAddress inet = InetAddress.getByName(host);
            final String addr = inet.getHostAddress();
            return addr;
        }
    }

    public static LocationFactory build() {
        try {
            return new OfflineLocationFactory();
        } catch (Exception ex) {
            return new NoopLocationFactory();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(new OfflineLocationFactory().make("54.195.196.98"));
    }
//    public static void main(String[] args) throws IOException {
    // }
}
