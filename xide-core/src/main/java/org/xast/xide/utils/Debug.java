package org.xast.xide.utils;

public class Debug {
    public static void info(Object message) {
        System.out.println("\u001B[34m[INFO] " + message + "\u001B[0m");
    }

    public static void warn(Object message) {
        System.out.println("\u001B[33m[WARN] " + message + "\u001B[0m");
    }

    public static void error(Object message) {
        System.err.println("\u001B[31m[ERROR] " + message + "\u001B[0m");
    }
}
