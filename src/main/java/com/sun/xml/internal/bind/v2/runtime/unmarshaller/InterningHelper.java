package com.sun.xml.internal.bind.v2.runtime.unmarshaller;

import java.util.concurrent.ConcurrentHashMap;

public class InterningHelper {
    private static final ConcurrentHashMap<String, String> internMap = new ConcurrentHashMap<String, String>(60000);

    public static String intern(String s) {
        String interned = internMap.get(s);
        if (interned == null) {
            interned = s.intern();
            internMap.put(interned, interned);
        }
        return interned;
    }
}
