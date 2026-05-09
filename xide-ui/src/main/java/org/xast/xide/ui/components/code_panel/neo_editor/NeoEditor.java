package org.xast.xide.ui.components.code_panel.neo_editor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;

import org.xast.xide.core.utils.Debug;
import org.xast.xide.ui.utils.XideStyle;

import lombok.Getter;

public class NeoEditor extends JComponent {
    private static final float FONT_SIZE = 17f;

    @Getter
    private Font font;
    private FontMetrics fm;
    private PieceTable pieceTable;
    private Caret caret;

    public NeoEditor(String content) {
        setLayout(null);

        pieceTable = new PieceTable(content);        
        caret = new Caret(this::repaint);

        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        setFont(
            XideStyle.getCurrent()
                .codeFont()
                .deriveFont(FONT_SIZE)
        );

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int charWidth = fm.charWidth('W');
                int lineHeight = fm.getHeight();

                caret.setX((x + 5) / charWidth);
                caret.setY(y / lineHeight);
                caret.setVisible(true);
            }
        });
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);

        this.font = font;
        this.fm = getFontMetrics(font);

        caret.setDeltaX(fm.charWidth('W'));
        caret.setDeltaY(fm.getHeight());
        caret.setHeight(fm.getHeight());
    }

    public String getContent() {
        return pieceTable.read()
            .stream()
            .collect(Collectors.joining("\n"));
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
        g2d.setFont(font);

        caret.paintComponent(g2d);
        
        List<String> lines = pieceTable.read();
        for (int i = 0; i < lines.size(); i++) {
            g2d.drawString(lines.get(i), 0, fm.getHeight() * i + 20);
        }
    }
}
