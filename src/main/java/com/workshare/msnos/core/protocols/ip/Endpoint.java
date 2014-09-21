package com.workshare.msnos.core.protocols.ip;


public interface Endpoint {

    public enum Type {
        UDP(0), HTTP(80), SSH(22);
    
        private short defaultPort;
    
        Type(int defaultPort) {
            this.defaultPort = (short) defaultPort;
        }
    
        public short defaultPort() {
            return defaultPort;
        }

        public String defaultUrl() {
            return this.name()+":"+defaultPort;
        }
    }

    public abstract Type getType();

    public abstract Network getNetwork();

    public abstract short getPort();
}