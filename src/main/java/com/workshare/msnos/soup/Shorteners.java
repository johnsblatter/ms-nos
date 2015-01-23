package com.workshare.msnos.soup;

import java.util.UUID;

/**
 * How to use this class? Simple, use import static. DO NOT use Shorteners.blah
 * Thanks
 * @author bossola
 *
 */
public class Shorteners {

    private Shorteners() {}

    public static final String shorten(long number) {
        final String s = String.format("%08d", number);
        final int l = s.length();
        return s.substring(l - 5, l);
    }

    public static final  String shorten(UUID uuid) {
        final String s = uuid.toString();
        final int l = s.length();
        return s.substring(l - 8, l);
    }
    
    public static final String shorten(String text, int length) {
        if (text == null)
            text =  "---";
        
        if (text.length() > length)
            return text.substring(0, length);
        else
            return text;
    }
}
