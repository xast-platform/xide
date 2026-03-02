package org.xast.xide.ui.utils;

import java.io.File;

public class FileIconProvider {
    public static LucideIcon getIconForFile(File file, int size) {
        if (file.isDirectory()) {
            return LucideIcon.FOLDER;
        }

        String name = file.getName().toLowerCase();
        if (name.endsWith(".rs")) 
            return LucideIcon.COMPONENT;
        if (name.endsWith(".hs")) 
            return LucideIcon.VARIABLE;
        if (name.endsWith(".toml") || name.endsWith(".ini")) 
            return LucideIcon.BOLT;
        if (name.endsWith(".xst")) 
            return LucideIcon.SCROLL_TEXT;
        if (name.endsWith(".xbc")) 
            return LucideIcon.PACKAGE;


        // default file icon
        return LucideIcon.FILE_SEARCH_CORNER;
    }
}
