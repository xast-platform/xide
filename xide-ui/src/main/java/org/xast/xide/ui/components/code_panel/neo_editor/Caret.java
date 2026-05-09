package org.xast.xide.ui.components.code_panel.neo_editor;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Timer;

import org.xast.xide.ui.components.RepaintRegion;

import lombok.*;

public class Caret {
    private static final int BLINK_INTERVAL_MS = 500;

    @Getter @Setter
    private int deltaX = 0;

    @Getter @Setter
    private int deltaY = 0;

    @Getter
    private int x = 0;

    @Getter
    private int y = 0;
    
    @Getter @Setter
    private int height = 20;

    @Getter
    private boolean visible = true;
    private final Timer timer;
    private final RepaintRegion repaintRegion;

    public Caret(RepaintRegion repaintRegion) {
        this.repaintRegion = repaintRegion;
        timer = new Timer(BLINK_INTERVAL_MS, e -> {
            visible = !visible;
            repaintCurrent();
        });
        timer.setInitialDelay(BLINK_INTERVAL_MS);
    }

    public void moveTo(int nextX, int nextY) {
        int previousX = x;
        int previousY = y;

        x = nextX;
        y = nextY;
        visible = true;

        repaintAt(previousX, previousY);
        repaintCurrent();

        timer.restart();
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            if (visible) {
                timer.restart();
            }

            return;
        }

        this.visible = visible;
        repaintCurrent();

        if (visible) {
            timer.restart();
        } else {
            timer.stop();
        }
    }

    public void paintComponent(Graphics g) {
        if (visible) {
            g.setColor(Color.WHITE);
            g.fillRect(x * deltaX, y * deltaY, 2, height);
        }
    }

    private void repaintCurrent() {
        repaintAt(x, y);
    }

    private void repaintAt(int x, int y) {
        repaintRegion.repaint(x * deltaX, y * deltaY, 2, height);
    }
}
