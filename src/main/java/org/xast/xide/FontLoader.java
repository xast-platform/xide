package org.xast.xide;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FontLoader {
    public Font loadFont(String path, float size) {
        try (InputStream is = getClass().getResourceAsStream("/"+path)) {
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}