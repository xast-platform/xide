package org.xast.xide.ui.utils;

import java.awt.image.BufferedImage;

import lombok.Getter;

public class Icon {
    @Getter
    private BufferedImage image;

    Icon(BufferedImage image) {
        this.image = image;
    }
}
