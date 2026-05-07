package org.xast.xide.ui.components.code_panel.neo_editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.stream.Collectors;

import javax.swing.JComponent;

public class NeoEditor extends JComponent {
    private PieceTable pieceTable;

    public NeoEditor(String content) {
        pieceTable = new PieceTable(content);
    }

    public String getContent() {
        return pieceTable.read()
            .stream()
            .collect(Collectors.joining("\n"));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(250, 200);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2d.setColor(Color.WHITE);
        g2d.drawString("This is my custom Panel!", 10, 20);
    }  
}
