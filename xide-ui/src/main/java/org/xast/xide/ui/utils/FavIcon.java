package org.xast.xide.ui.utils;

import java.awt.image.BufferedImage;

import lombok.Getter;

public class FavIcon {
    @Getter
    private BufferedImage image;

    FavIcon(BufferedImage image) {
        this.image = image;
    }
}
