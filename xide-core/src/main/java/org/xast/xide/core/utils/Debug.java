package org.xast.xide.core.utils;

public class Debug {
    
    public static void info(Object message) {
        System.out.println("[INFO] " + message);
    }

    public static void warn(Object message) {
        System.out.println("[WARN] " + message);
    }

    public static void error(Object message) {
        System.err.println("[ERROR] " + message);
    }
}
