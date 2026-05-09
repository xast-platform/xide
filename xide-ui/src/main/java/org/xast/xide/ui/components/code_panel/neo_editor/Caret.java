package org.xast.xide.ui.components.code_panel.neo_editor;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Timer;

import org.xast.xide.ui.components.RepaintRegion;

import lombok.*;

public class Caret {
    @Getter @Setter
    private int deltaX = 0;

    @Getter @Setter
    private int deltaY = 0;

    @Getter @Setter
    private int x = 0;

    @Getter @Setter
    private int y = 0;
    
    @Getter @Setter
    private int height = 20;

    @Getter
    private boolean visible = true;
    private Timer timer;

    public Caret(RepaintRegion repaintRegion) {
        timer = new Timer(500, e -> {
            visible = !visible;
            repaintRegion.repaint(0, 0, 1000, 1000);
        });

        timer.start();
    }

    public void setVisible(boolean visible) {
        if (visible) {
            timer.stop();
        }

        this.visible = visible;
        this.timer.restart();
    }

    public void paintComponent(Graphics g) {
        if (visible) {
            g.setColor(Color.WHITE);
            g.fillRect(x * deltaX, y * deltaY, 2, height);
        }
    }
}
