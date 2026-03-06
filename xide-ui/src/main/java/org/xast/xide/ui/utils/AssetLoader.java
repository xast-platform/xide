package org.xast.xide.ui.utils;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import com.formdev.flatlaf.extras.FlatSVGIcon;

public class AssetLoader {
    private static AssetLoader instance;

    private AssetLoader() {}

    public static synchronized AssetLoader getInstance() {
        if (instance == null) {
            instance = new AssetLoader();
        }
        return instance;
    }

    public synchronized Font loadFont(String path, float size) {
        try (InputStream is = getClass().getResourceAsStream("/" + path)) {
            if (is == null) {
                throw new IllegalStateException("Font resource not found: " + path);
            }
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load font: " + path, e);
        }
    }

    public synchronized FavIcon loadIcon(String path) {
        FlatSVGIcon svgIcon = new FlatSVGIcon(
            "icons/logo.svg", 
            XideStyle.ICON_WIDTH,
            XideStyle.ICON_HEIGHT
        );
        BufferedImage img = new BufferedImage(
            svgIcon.getIconWidth(),
            svgIcon.getIconHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = img.createGraphics();
        svgIcon.paintIcon(null, g, 0, 0);
        g.dispose();

        return new FavIcon(img);
    }
}
