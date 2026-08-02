package org.xast.xide.ui.components.code_panel.neo_editor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.Timer;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.ThemeChangedEvent;
import org.xast.xide.core.utils.Debug;
import org.xast.xide.ui.components.code_panel.neo_editor.PieceTable.Position;
import org.xast.xide.ui.utils.XideStyle;

import io.github.treesitter.jtreesitter.Language;
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
    private boolean hasSelection = false;
    private int selAnchorX, selAnchorY;
    private boolean draggingSelection = false;
    private int desiredColumn = -1;

    @Getter
    private Font font;
    private Color fontColor;
    private FontMetrics fm;
    private XideStyle style;

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
        EventBus eventBus,
        String content,
        NeoEditorStatus editorStatus,
        TextChangeListener textChangeListener
    ) {
        setLayout(null);
        setFocusable(true);

        SymbolLookup lookup = SymbolLookup.libraryLookup("tree-sitter-java", Arena.global());
        try {
            var lang = Language.load(lookup, "tree_sitter_java");
        } catch (RuntimeException e) {
            Debug.error(e.getMessage());
        }

        pieceTable = new PieceTable(content);
        caret = new Caret(eventBus, (x, y, w, h) -> needsRepaint = true);
        frameTimer = new Timer(FRAME_INTERVAL_MS, e -> {
            if (needsRepaint) {
                needsRepaint = false;
                repaint();
            }
        });
        frameTimer.start();

        style = XideStyle.getCurrent();
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        applyTheme();
        eventBus.subscribe(ThemeChangedEvent.class, e -> applyTheme());
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

                Pos clicked = pointToPosition(x, y);

                if (e.isShiftDown()) {
                    beginSelectionIfNeeded();
                    caret.moveTo(clicked.col(), clicked.line());
                    hasSelection = !clicked.equals(anchorPos());
                } else {
                    clearSelection();
                    selAnchorX = clicked.col();
                    selAnchorY = clicked.line();
                    caret.moveTo(clicked.col(), clicked.line());
                    draggingSelection = true;
                }

                editorStatus.setCurrentChar(clicked.col() + 1);
                editorStatus.setCurrentLine(clicked.line() + 1);
                ensureCaretVisible();
                needsRepaint = true;
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
                if (draggingScrollbar) {
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
                    return;
                }

                if (draggingSelection) {
                    Pos pos = pointToPosition(e.getX(), e.getY());
                    caret.moveTo(pos.col(), pos.line());
                    hasSelection = !pos.equals(anchorPos());
                    editorStatus.setCurrentChar(pos.col() + 1);
                    editorStatus.setCurrentLine(pos.line() + 1);
                    ensureCaretVisible();
                    needsRepaint = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingScrollbar = false;
                draggingSelection = false;
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
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();

                if (c == KeyEvent.CHAR_UNDEFINED || Character.isISOControl(c)) {
                    return;
                }
                if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
                    return;
                }

                if (hasSelection) {
                    deleteSelection();
                }
                insertChar(c);
                textChangeListener.accept();

                ensureCaretVisible();
                needsRepaint = true;
            }

            @Override
            public void keyPressed(KeyEvent e) {
                boolean textChanged = false;
                boolean shift = e.isShiftDown();
                boolean ctrl = e.isControlDown();

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_BACK_SPACE -> {
                        if (hasSelection) deleteSelection(); else deleteBackward();
                        textChanged = true;
                    }
                    case KeyEvent.VK_DELETE -> {
                        if (hasSelection) deleteSelection(); else deleteForward();
                        textChanged = true;
                    }
                    case KeyEvent.VK_ENTER -> {
                        if (hasSelection) deleteSelection();
                        insertNewline();
                        textChanged = true;
                    }
                    case KeyEvent.VK_LEFT -> {
                        if (shift) beginSelectionIfNeeded(); else clearSelection();
                        moveCaretLeft();
                        if (shift) hasSelection = true;
                    }
                    case KeyEvent.VK_RIGHT -> {
                        if (shift) beginSelectionIfNeeded(); else clearSelection();
                        moveCaretRight();
                        if (shift) hasSelection = true;
                    }
                    case KeyEvent.VK_DOWN -> {
                        if (shift) beginSelectionIfNeeded(); else clearSelection();
                        moveCaretVertical(1);
                        if (shift) hasSelection = true;
                    }
                    case KeyEvent.VK_UP -> {
                        if (shift) beginSelectionIfNeeded(); else clearSelection();
                        moveCaretVertical(-1);
                        if (shift) hasSelection = true;
                    }
                    case KeyEvent.VK_HOME -> {
                        if (shift) beginSelectionIfNeeded(); else clearSelection();
                        moveCaretHome();
                        if (shift) hasSelection = true;
                    }
                    case KeyEvent.VK_END -> {
                        if (shift) beginSelectionIfNeeded(); else clearSelection();
                        moveCaretEnd();
                        if (shift) hasSelection = true;
                    }
                    case KeyEvent.VK_A -> {
                        if (ctrl) {
                            selAnchorX = 0;
                            selAnchorY = 0;
                            int lastLine = totalLines - 1;
                            caret.moveTo(cachedLines.get(lastLine).length(), lastLine);
                            hasSelection = true;
                        }
                    }
                    case KeyEvent.VK_C -> { if (ctrl) copySelection(); }
                    case KeyEvent.VK_X -> {
                        if (ctrl && hasSelection) {
                            copySelection();
                            deleteSelection();
                            textChanged = true;
                        }
                    }
                    case KeyEvent.VK_V -> {
                        if (ctrl) {
                            pasteClipboard();
                            textChanged = true;
                        }
                    }
                    case KeyEvent.VK_SHIFT, KeyEvent.VK_CAPS_LOCK, KeyEvent.VK_F12,
                        KeyEvent.VK_CONTROL, KeyEvent.VK_ALT -> {}
                    default -> {}
                }

                if (textChanged) {
                    textChangeListener.accept();
                }

                ensureCaretVisible();
                needsRepaint = true;
            }
        });
    }

    private record Pos(int line, int col) implements Comparable<Pos> {
        public int compareTo(Pos o) {
            int c = Integer.compare(line, o.line);
            return c != 0 ? c : Integer.compare(col, o.col);
        }
    }

    private Pos anchorPos() { return new Pos(selAnchorY, selAnchorX); }
    private Pos caretPos()  { return new Pos(caret.getY(), caret.getX()); }
    private Pos selStart()  { return anchorPos().compareTo(caretPos()) <= 0 ? anchorPos() : caretPos(); }
    private Pos selEnd()    { return anchorPos().compareTo(caretPos()) <= 0 ? caretPos() : anchorPos(); }

    private Pos pointToPosition(int x, int y) {
        int charWidth = fm.charWidth('W');
        int lineHeight = fm.getHeight();

        int adjustedX = x - gutterWidth;
        int adjustedY = y + scrollY;

        int line = Math.max(0, Math.min(adjustedY / lineHeight, totalLines - 1));
        int rawCh = Math.max(0, (adjustedX + 5) / charWidth);
        int lineLength = line < cachedLines.size() ? cachedLines.get(line).length() : 0;
        int ch = Math.max(0, Math.min(rawCh, lineLength));

        return new Pos(line, ch);
    }

    private int lineLength(int line) {
        return line >= 0 && line < cachedLines.size() ? cachedLines.get(line).length() : 0;
    }

    private void moveCaretLeft() {
        int x = caret.getX();
        int y = caret.getY();
        if (x > 0) {
            caret.moveTo(x - 1, y);
        } else if (y > 0) {
            caret.moveTo(lineLength(y - 1), y - 1);
        }
        desiredColumn = -1;
    }

    private void moveCaretRight() {
        int x = caret.getX();
        int y = caret.getY();
        if (x < lineLength(y)) {
            caret.moveTo(x + 1, y);
        } else if (y < totalLines - 1) {
            caret.moveTo(0, y + 1);
        }
        desiredColumn = -1;
    }

    private void moveCaretVertical(int deltaLine) {
        int targetLine = Math.max(0, Math.min(totalLines - 1, caret.getY() + deltaLine));
        int column = desiredColumn >= 0 ? desiredColumn : caret.getX();
        desiredColumn = column; // preserve across consecutive vertical moves
        int clampedColumn = Math.min(column, lineLength(targetLine));
        caret.moveTo(clampedColumn, targetLine);
    }

    private void moveCaretHome() {
        caret.moveTo(0, caret.getY());
        desiredColumn = -1;
    }

    private void moveCaretEnd() {
        caret.moveTo(lineLength(caret.getY()), caret.getY());
        desiredColumn = -1;
    }

    private String getSelectedText() {
        Pos start = selStart();
        Pos end = selEnd();
        if (start.line() == end.line()) {
            return cachedLines.get(start.line()).substring(start.col(), end.col());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(cachedLines.get(start.line()).substring(start.col())).append('\n');
        for (int line = start.line() + 1; line < end.line(); line++) {
            sb.append(cachedLines.get(line)).append('\n');
        }
        sb.append(cachedLines.get(end.line()), 0, end.col());
        return sb.toString();
    }

    private void copySelection() {
        if (!hasSelection) return;
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(getSelectedText()), null);
    }

    private void pasteClipboard() {
        try {
            String text = (String) Toolkit.getDefaultToolkit()
                .getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (hasSelection) deleteSelection();
            insertText(text);
        } catch (Exception ignored) {}
    }

    private void insertText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') insertNewline();
            else if (c != '\r') insertChar(c);
        }
    }

    private int charOffsetOf(Pos p) {
        int offset = 0;
        for (int i = 0; i < p.line(); i++) {
            offset += cachedLines.get(i).length() + 1;
        }
        return offset + p.col();
    }

    private void deleteSelection() {
        if (!hasSelection) return;
        Pos start = selStart();
        Pos end = selEnd();
        int count = charOffsetOf(end) - charOffsetOf(start);

        caret.moveTo(end.col(), end.line());
        for (int i = 0; i < count; i++) {
            deleteBackward();
        }
        clearSelection();
    }

    private void beginSelectionIfNeeded() {
        if (!hasSelection) {
            selAnchorX = caret.getX();
            selAnchorY = caret.getY();
        }
    }

    private void clearSelection() {
        hasSelection = false;
    }

    private void applyTheme() {
        fontColor = style.isDarkTheme()
            ? Color.WHITE
            : Color.BLACK;
        setFont(style.codeFont().deriveFont(FONT_SIZE));
        setBackground(style.shiftAccent(0.3f));
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

        paintSelection(contentG, firstVisibleLine, lastVisibleLine, lineHeight);
        caret.paintComponent(contentG);

        contentG.setColor(fontColor);
        for (int i = 0; i < visibleLines.size(); i++) {
            int lineIndex = firstVisibleLine + i;
            int baselineY = lineHeight * lineIndex + fm.getAscent();
            contentG.drawString(visibleLines.get(i), 0, baselineY);
        }
        contentG.dispose();

        paintGutter(g2d, firstVisibleLine, lastVisibleLine, lineHeight);
        paintScrollbar(g2d, width, height, lineHeight);
    }

    private void paintSelection(Graphics2D g, int firstVisibleLine, int lastVisibleLine, int lineHeight) {
        if (!hasSelection) return;

        Pos start = selStart();
        Pos end = selEnd();

        g.setColor(new Color(80, 140, 255, 70));
        int from = Math.max(start.line(), firstVisibleLine);
        int to = Math.min(end.line(), lastVisibleLine - 1);

        for (int line = from; line <= to; line++) {
            String text = line < cachedLines.size() ? cachedLines.get(line) : "";
            int colStart = (line == start.line()) ? start.col() : 0;
            int colEnd = (line == end.line()) ? end.col() : text.length();

            int x1 = fm.stringWidth(text.substring(0, Math.min(colStart, text.length())));
            int x2 = fm.stringWidth(text.substring(0, Math.min(colEnd, text.length())));
            int width = Math.max(x2 - x1, colEnd > colStart ? 0 : 6);

            g.fillRect(x1, lineHeight * line, Math.max(width, 6), lineHeight);
        }
    }

    private void paintGutter(Graphics2D g2d, int firstVisibleLine, int lastVisibleLine, int lineHeight) {
        Color base = getBackground();
        Color lineNumberColor = new Color(fontColor.getRed(), fontColor.getGreen(), fontColor.getBlue(), 140);

        g2d.setColor(base);
        g2d.fillRect(0, 0, gutterWidth, getHeight());

        g2d.setFont(font);
        g2d.setColor(lineNumberColor);

        for (int lineIndex = firstVisibleLine; lineIndex < lastVisibleLine; lineIndex++) {
            String label = String.valueOf(lineIndex + 1);
            int textWidth = fm.stringWidth(label);
            int y = lineHeight * lineIndex - scrollY + fm.getAscent();
            g2d.drawString(label, gutterWidth - textWidth - GUTTER_RIGHT_MARGIN, y);
        }

        g2d.setColor(style.shiftAccent(0.15f));
        g2d.drawLine(gutterWidth - 1, 0, gutterWidth - 1, getHeight());
    }

    private void paintScrollbar(Graphics2D g2d, int width, int height, int lineHeight) {
        int contentHeight = Math.max(1, totalLines * lineHeight);
        if (contentHeight <= height) {
            return;
        }

        int trackX = width - SCROLLBAR_WIDTH;
        g2d.setColor(style.isDarkTheme() ? new Color(255, 255, 255, 30) : new Color(0, 0, 0, 40));
        g2d.fillRect(trackX, 0, SCROLLBAR_WIDTH, height);

        int thumbHeight = computeThumbHeight(height, contentHeight);
        int maxScrollY = contentHeight - height;
        int maxThumbY = height - thumbHeight;
        int thumbY = maxScrollY <= 0 ? 0 : (int) ((long) scrollY * maxThumbY / maxScrollY);

        g2d.setColor(
            hoveringScrollbar || draggingScrollbar
                ? style.shiftAccent(0.5f)
                : style.shiftAccent(0.35f)
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