package org.xast.xide.ui.components.code_panel.neo_editor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.Timer;
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

    private static final float FONT_SIZE = 19f;
    private static final int SCROLLBAR_WIDTH = 16;
    private static final int SCROLLBAR_MIN_THUMB = 20;
    private static final int GUTTER_PADDING = 32;
    private static final int GUTTER_RIGHT_MARGIN = 28;
    private static final int SCROLL_LINES_PER_NOTCH = 2;
    private static final int FRAME_INTERVAL_MS = 1000 / 60;

    private final Timer frameTimer;
    private boolean needsRepaint = true;
    private boolean hoveringScrollbar = false;

    @Getter
    private Font font;
    private FontMetrics fm;

    private PieceTable pieceTable;
    private Caret caret;
    private List<String> cachedLines;
    private int totalLines = 1;
    private int gutterWidth = 40;
    private int scrollY = 0;

    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int dragStartScrollY;

    public NeoEditor(
        String content,
        NeoEditorStatus editorStatus,
        TextChangeListener textChangeListener
    ) {
        setLayout(null);
        setFocusable(true);

        pieceTable = new PieceTable(content);
        caret = new Caret((x, y, w, h) -> needsRepaint = true);
        frameTimer = new Timer(FRAME_INTERVAL_MS, e -> {
            if (needsRepaint) {
                needsRepaint = false;
                repaint();
            }
        });
        frameTimer.start();

        XideStyle style = XideStyle.getCurrent();

        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        setFont(style.codeFont().deriveFont(FONT_SIZE));
        setBackground(UIManager.getColor("TextArea.background"));

        refreshMetrics();

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocus();
                int x = e.getX();
                int y = e.getY();

                if (isScrollbarVisible() && isOverScrollbar(x)) {
                    beginScrollbarDrag(y);
                    return;
                }

                moveCaretToPoint(x, y, editorStatus);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                boolean nowHovering = isScrollbarVisible() && isOverScrollbar(e.getX());
                if (nowHovering != hoveringScrollbar) {
                    hoveringScrollbar = nowHovering;
                    needsRepaint = true;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!draggingScrollbar) {
                    return;
                }

                int lineHeight = fm.getHeight();
                int contentHeight = totalLines * lineHeight;
                int trackHeight = getHeight();
                int maxScrollY = Math.max(0, contentHeight - trackHeight);

                if (maxScrollY == 0) {
                    return;
                }

                int thumbHeight = computeThumbHeight(trackHeight, contentHeight);
                int maxThumbY = Math.max(1, trackHeight - thumbHeight);
                int deltaY = e.getY() - dragStartY;

                int deltaScroll = (int) ((long) deltaY * maxScrollY / maxThumbY);
                setScrollY(dragStartScrollY + deltaScroll);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingScrollbar = false;
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int lineHeight = fm.getHeight();
                setScrollY(scrollY + e.getWheelRotation() * lineHeight * SCROLL_LINES_PER_NOTCH);
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                clampScrollY();
                needsRepaint = true;
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Debug.info(e.paramString());
                boolean textChanged = false;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_BACK_SPACE -> {
                        deleteBackward();
                        textChanged = true;
                    }
                    case KeyEvent.VK_DELETE -> {
                        deleteForward();
                        textChanged = true;
                    }
                    case KeyEvent.VK_ENTER -> {
                        insertNewline();
                        textChanged = true;
                    }
                    case KeyEvent.VK_LEFT ->
                        caret.moveTo(Math.max(0, caret.getX() - 1), caret.getY());
                    case KeyEvent.VK_RIGHT ->
                        caret.moveTo(caret.getX() + 1, caret.getY());
                    case KeyEvent.VK_DOWN ->
                        caret.moveTo(caret.getX(), caret.getY() + 1);
                    case KeyEvent.VK_UP ->
                        caret.moveTo(caret.getX(), Math.max(0, caret.getY() - 1));
                    case KeyEvent.VK_SHIFT -> {}
                    case KeyEvent.VK_CAPS_LOCK -> {}
                    case KeyEvent.VK_F12 -> {}
                    default -> {
                        if (!e.isControlDown() && !e.isAltDown() && e.getKeyChar() >= 32) {
                            insertChar(e.getKeyChar());
                            textChanged = true;
                        }
                    }
                }

                if (textChanged) {
                    textChangeListener.accept();
                }

                ensureCaretVisible();
                needsRepaint = true;
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int lineHeight = fm.getHeight();

        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, width, height);

        int firstVisibleLine = Math.max(0, scrollY / lineHeight);
        int visibleLineSlots = height / lineHeight + 2;
        int lastVisibleLine = Math.min(totalLines, firstVisibleLine + visibleLineSlots);

        List<String> visibleLines = firstVisibleLine < cachedLines.size()
            ? cachedLines.subList(firstVisibleLine, Math.min(lastVisibleLine, cachedLines.size()))
            : List.of();

        int contentX = gutterWidth;
        int contentWidth = Math.max(0, width - gutterWidth - SCROLLBAR_WIDTH);

        Graphics2D contentG = (Graphics2D) g2d.create(contentX, 0, contentWidth, height);
        contentG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        contentG.setFont(font);
        contentG.translate(0, -scrollY);

        caret.paintComponent(contentG);

        contentG.setColor(Color.WHITE);
        for (int i = 0; i < visibleLines.size(); i++) {
            int lineIndex = firstVisibleLine + i;
            int baselineY = lineHeight * lineIndex + fm.getAscent();
            contentG.drawString(visibleLines.get(i), 0, baselineY);
        }
        contentG.dispose();

        paintGutter(g2d, firstVisibleLine, lastVisibleLine, lineHeight);
        paintScrollbar(g2d, width, height, lineHeight);
    }

    private void paintGutter(Graphics2D g2d, int firstVisibleLine, int lastVisibleLine, int lineHeight) {
        Color base = getBackground();
        Color gutterBg = base != null ? base.darker() : new Color(30, 30, 30);

        g2d.setColor(base);
        g2d.fillRect(0, 0, gutterWidth, getHeight());

        g2d.setFont(font);
        g2d.setColor(new Color(140, 140, 140));

        for (int lineIndex = firstVisibleLine; lineIndex < lastVisibleLine; lineIndex++) {
            String label = String.valueOf(lineIndex + 1);
            int textWidth = fm.stringWidth(label);
            int y = lineHeight * lineIndex - scrollY + fm.getAscent();
            g2d.drawString(label, gutterWidth - textWidth - GUTTER_RIGHT_MARGIN, y);
        }

        g2d.setColor(gutterBg.brighter());
        g2d.drawLine(gutterWidth - 1, 0, gutterWidth - 1, getHeight());
    }

    private void paintScrollbar(Graphics2D g2d, int width, int height, int lineHeight) {
        int contentHeight = Math.max(1, totalLines * lineHeight);
        if (contentHeight <= height) {
            return;
        }

        int trackX = width - SCROLLBAR_WIDTH;
        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.fillRect(trackX, 0, SCROLLBAR_WIDTH, height);

        int thumbHeight = computeThumbHeight(height, contentHeight);
        int maxScrollY = contentHeight - height;
        int maxThumbY = height - thumbHeight;
        int thumbY = maxScrollY <= 0 ? 0 : (int) ((long) scrollY * maxThumbY / maxScrollY);

        g2d.setColor(
            hoveringScrollbar || draggingScrollbar
                ? new Color(180, 180, 180, 220)
                : new Color(150, 150, 150, 180)
        );
        g2d.fillRect(trackX + 2, thumbY, SCROLLBAR_WIDTH - 4, thumbHeight);
    }

    private int computeThumbHeight(int trackHeight, int contentHeight) {
        return Math.max(SCROLLBAR_MIN_THUMB, (int) ((long) trackHeight * trackHeight / contentHeight));
    }

    private boolean isScrollbarVisible() {
        return totalLines * fm.getHeight() > getHeight();
    }

    private boolean isOverScrollbar(int x) {
        return x >= getWidth() - SCROLLBAR_WIDTH;
    }

    private void beginScrollbarDrag(int clickY) {
        int lineHeight = fm.getHeight();
        int contentHeight = totalLines * lineHeight;
        int trackHeight = getHeight();
        int thumbHeight = computeThumbHeight(trackHeight, contentHeight);
        int maxThumbY = Math.max(1, trackHeight - thumbHeight);
        int maxScrollY = Math.max(0, contentHeight - trackHeight);

        int desiredThumbY = Math.max(0, Math.min(maxThumbY, clickY - thumbHeight / 2));
        setScrollY(maxThumbY == 0 ? 0 : (int) ((long) desiredThumbY * maxScrollY / maxThumbY));

        draggingScrollbar = true;
        dragStartY = clickY;
        dragStartScrollY = scrollY;
    }

    private void moveCaretToPoint(int x, int y, NeoEditorStatus editorStatus) {
        int charWidth = fm.charWidth('W');
        int lineHeight = fm.getHeight();

        int adjustedX = x - gutterWidth;
        int adjustedY = y + scrollY;

        int line = Math.max(0, Math.min(adjustedY / lineHeight, totalLines - 1));
        int rawCh = Math.max(0, (adjustedX + 5) / charWidth);
        int lineLength = line < cachedLines.size() ? cachedLines.get(line).length() : 0;
        int ch = Math.max(0, Math.min(rawCh, lineLength));

        caret.moveTo(ch, line);
        editorStatus.setCurrentChar(ch + 1);
        editorStatus.setCurrentLine(line + 1);

        ensureCaretVisible();
        needsRepaint = true;
    }

    private void ensureCaretVisible() {
        int lineHeight = fm.getHeight();
        int caretTop = caret.getY() * lineHeight;
        int caretBottom = caretTop + lineHeight;

        if (caretTop < scrollY) {
            scrollY = caretTop;
        } else if (caretBottom > scrollY + getHeight()) {
            scrollY = caretBottom - getHeight();
        }

        clampScrollY();
    }

    private void setScrollY(int value) {
        scrollY = value;
        clampScrollY();
        needsRepaint = true;
    }

    private void clampScrollY() {
        int lineHeight = fm.getHeight();
        int maxScrollY = Math.max(0, totalLines * lineHeight - getHeight());
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY));
    }

    private void refreshMetrics() {
        cachedLines = pieceTable.read();
        totalLines = Math.max(1, cachedLines.size());
        updateGutterWidth();
        clampScrollY();
    }

    private void updateGutterWidth() {
        int digits = Math.max(2, String.valueOf(totalLines).length());
        gutterWidth = GUTTER_PADDING + digits * fm.charWidth('0') + GUTTER_RIGHT_MARGIN;
    }

    private void insertChar(char ch) {
        int x = caret.getX();
        int y = caret.getY();

        pieceTable.insert(String.valueOf(ch), new Position(y, x));

        String line = cachedLines.get(y);
        cachedLines.set(y, line.substring(0, x) + ch + line.substring(x));

        caret.moveTo(x + 1, y);
    }

    private void insertNewline() {
        int x = caret.getX();
        int y = caret.getY();

        pieceTable.insert("\n", new Position(y, x));

        String line = cachedLines.get(y);
        cachedLines.set(y, line.substring(0, x));
        cachedLines.add(y + 1, line.substring(x));

        totalLines++;
        updateGutterWidth();
        clampScrollY();

        caret.moveTo(0, y + 1);
    }

    private void deleteBackward() {
        int x = caret.getX();
        int y = caret.getY();

        pieceTable.delete(new Position(y, x));

        if (x > 0) {
            String line = cachedLines.get(y);
            cachedLines.set(y, line.substring(0, x - 1) + line.substring(x));
            caret.moveTo(x - 1, y);
        } else if (y > 0) {
            String prevLine = cachedLines.get(y - 1);
            String curLine = cachedLines.remove(y);
            cachedLines.set(y - 1, prevLine + curLine);
            totalLines--;
            updateGutterWidth();
            clampScrollY();
            caret.moveTo(prevLine.length(), y - 1);
        }
    }

    private void deleteForward() {
        int x = caret.getX();
        int y = caret.getY();

        String line = cachedLines.get(y);

        if (x < line.length()) {
            pieceTable.delete(new Position(y, x + 1));
            cachedLines.set(y, line.substring(0, x) + line.substring(x + 1));
        } else if (y < cachedLines.size() - 1) {
            pieceTable.delete(new Position(y + 1, 0));
            String nextLine = cachedLines.remove(y + 1);
            cachedLines.set(y, line + nextLine);
            totalLines--;
            updateGutterWidth();
            clampScrollY();
        }
    }
}