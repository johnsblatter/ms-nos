package com.workshare.msnos.core;

import com.workshare.msnos.soup.json.Json;

public class Version {

    public static final Version V1_0 = new Version(1,0);
    
    private final int major;
    private final int minor;
    
    public Version(int major, int minor) {
        super();
        this.major = major;
        this.minor = minor;
    }
    
    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }
   
    public String toString() {
        return Json.toJsonString(this);
    }

}
