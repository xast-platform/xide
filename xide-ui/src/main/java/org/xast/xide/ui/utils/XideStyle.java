package org.xast.xide.ui.utils;

import java.awt.Font;

public record XideStyle(
    Font uiFont,
    Icon favicon
) {
    public static final int ICON_WIDTH = 18;
    public static final int ICON_HEIGHT = 18;
    public static final String DEFAULT_UI_FONT = "fonts/selawik/selawk.ttf";
    public static final String DEFAULT_ICON = "icons/logo.svg";

    public static XideStyle defaultStyle() {
        AssetLoader assetLoader = AssetLoader.getInstance();
        Icon favicon = assetLoader.loadIcon(DEFAULT_ICON);
        Font uiFont = assetLoader.loadFont(DEFAULT_UI_FONT, 16f);

        return new XideStyle(
            uiFont, 
            favicon
        );
    }
}
