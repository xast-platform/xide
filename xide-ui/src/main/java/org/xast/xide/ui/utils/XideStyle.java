package org.xast.xide.ui.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import lombok.Setter;

public record XideStyle(
    Font uiFont,
    Font codeFont,
    Icon favicon,
    Dimension windowSize
) {
    @Setter
    private static XideStyle current;

    public static final int SIDEBAR_WIDTH = 150;
    public static final int BOTTOM_BAR_HEIGHT = 110;
    public static final int WINDOW_WIDTH = 1600;
    public static final int WINDOW_HEIGHT = 900;
    public static final int ICON_WIDTH = 18;
    public static final int ICON_HEIGHT = 18;
    public static final String DEFAULT_UI_FONT = "fonts/inter/inter.ttf";
    public static final String DEFAULT_CODE_FONT = "fonts/jetbrains_mono/JetBrainsMono-Regular.ttf";
    public static final String DEFAULT_ICON = "icons/logo.svg";

    public static XideStyle getCurrent() {
        if (current == null) {
            throw new IllegalStateException("XideStyle not set. Call XideStyle.setCurrent() before accessing the current style.");
        }
        
        return current;
    }

    public static XideStyle defaultStyle() {
        AssetLoader assetLoader = AssetLoader.getInstance();
        Icon favicon = assetLoader.loadIcon(DEFAULT_ICON);
        Font uiFont = assetLoader.loadFont(DEFAULT_UI_FONT, 16f);
        Font codeFont = assetLoader.loadFont(DEFAULT_CODE_FONT, 14f);
        Dimension windowSize = new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT);

        return new XideStyle(
            uiFont, 
            codeFont,
            favicon,
            windowSize
        );
    }

    public static Color lighten(Color color, float fraction) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        r += (int)((255 - r) * fraction);
        g += (int)((255 - g) * fraction);
        b += (int)((255 - b) * fraction);

        return new Color(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }
}
