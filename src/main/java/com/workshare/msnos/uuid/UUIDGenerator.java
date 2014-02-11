package com.workshare.msnos.uuid;

import java.util.UUID;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

public class UUIDGenerator {
    
    private static TimeBasedGenerator generator;
    static {
        generator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
    }
    
    public UUID generateString() {
        return generator.generate();
    }
}
