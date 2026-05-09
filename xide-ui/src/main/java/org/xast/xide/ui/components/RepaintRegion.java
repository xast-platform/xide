package org.xast.xide.ui.components;

@FunctionalInterface
public interface RepaintRegion {
    void repaint(int x, int y, int w, int h);
}