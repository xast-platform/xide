package org.xast.xide.ui.utils;

import java.awt.Color;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;

public enum LucideIcon {
    FOLDER_TREE("folder-tree"),
    FILE_SEARCH_CORNER("file-search-corner"),
    COG("cog"),
    BOLT("bolt"),
    COMPONENT("component"),
    FILE("file"),
    FOLDER("folder"),
    PACKAGE("package"),
    PARENTHESES("parentheses"),
    SCROLL_TEXT("scroll-text"),
    VARIABLE("variable"),
    X("x");

    // Other icons

    private String name;

    LucideIcon(String name) {
        this.name = name;
    }

    public FlatSVGIcon icon(int size, Color tint) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/lucide/" + name + ".svg", size, size);
        icon.setColorFilter(new ColorFilter(color -> tint));
        return icon;
    }
}
