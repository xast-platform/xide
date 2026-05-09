package org.xast.xide.ui.components.code_panel.neo_editor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.UIManager;

import org.xast.xide.core.utils.Debug;
import org.xast.xide.ui.components.code_panel.neo_editor.PieceTable.Position;
import org.xast.xide.ui.utils.XideStyle;

import lombok.Getter;

public class NeoEditor extends JComponent {
    @FunctionalInterface
    public interface TextChangeListener {
        void accept();
    }
    
    private static final float FONT_SIZE = 18f;

    @Getter
    private Font font;
    private FontMetrics fm;

    private boolean contentDirty = true;
    private List<String> cachedLines;
    private PieceTable pieceTable;
    private Caret caret;

    public NeoEditor(
        String content, 
        NeoEditorStatus editorStatus,
        TextChangeListener textChangeListener
    ) {
        setLayout(null);
        setFocusable(true);

        pieceTable = new PieceTable(content);
        caret = new Caret(this::repaint);
        cachedLines = getLines();

        XideStyle style = XideStyle.getCurrent();

        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        setFont(style.codeFont().deriveFont(FONT_SIZE));
        setBackground(UIManager.getColor("TextArea.background"));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocus();
                int x = e.getX();
                int y = e.getY();
                int charWidth = fm.charWidth('W');
                int lineHeight = fm.getHeight();
                int line = y / lineHeight;
                int ch = (x + 5) / charWidth;

                caret.moveTo(ch, line);
                editorStatus.setCurrentChar(ch + 1);
                editorStatus.setCurrentLine(line + 1);
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Debug.info(e.paramString());

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_BACK_SPACE -> {
                        pieceTable.delete(new Position(caret.getY(), caret.getX()));
                        caret.moveTo(Math.max(0, caret.getX() - 1), caret.getY());
                        contentDirty = true;
                    }
                    case KeyEvent.VK_DELETE -> {
                        pieceTable.delete(new Position(caret.getY(), caret.getX()));
                        contentDirty = true;
                    }
                    case KeyEvent.VK_ENTER -> {
                        pieceTable.insert("\n", new Position(caret.getY(), caret.getX()));
                        caret.moveTo(0, caret.getY() + 1);
                        contentDirty = true;
                    }
                    case KeyEvent.VK_LEFT -> {
                        caret.moveTo(Math.max(0, caret.getX() - 1), caret.getY());
                        contentDirty = true;
                    }
                    case KeyEvent.VK_RIGHT -> {
                        caret.moveTo(caret.getX() + 1, caret.getY());
                        contentDirty = true;
                    }
                    default -> {
                        // Regular character
                        if (!e.isControlDown() && !e.isAltDown() && e.getKeyChar() >= 32) {
                            pieceTable.insert(String.valueOf(e.getKeyChar()), new Position(caret.getY(), caret.getX()));
                            caret.moveTo(caret.getX() + 1, caret.getY());
                            contentDirty = true;
                        }
                    }
                };
                
                if (contentDirty) {
                    textChangeListener.accept();
                    repaint();   
                }
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

        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.WHITE);
        g2d.setFont(font);

        caret.paintComponent(g2d);
        
        List<String> lines = getLines();
        for (int i = 0; i < lines.size(); i++) {
            g2d.drawString(lines.get(i), 0, fm.getHeight() * i + 20);
        }
    }

    private List<String> getLines() {
        if (contentDirty) {
            cachedLines = pieceTable.read();
            contentDirty = false;
        }
        return cachedLines;
    }
}
