package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

import javax.swing.JComponent
import javax.swing.Timer

import org.xast.xide.core.event.EventBus
import org.xast.xide.core.event.ThemeChangedEvent
import org.xast.xide.ui.components.code_panel.neo_editor.PieceTable
import org.xast.xide.ui.utils.XideStyle
import scala.collection.mutable.ArrayBuffer

object NeoEditor:
    trait TextChangeListener:
        def accept(): Unit

    private val FONT_SIZE: Float = 20f
    private val SCROLLBAR_WIDTH: Int = 16
    private val SCROLLBAR_MIN_THUMB: Int = 20
    private val GUTTER_PADDING: Int = 32
    private val GUTTER_RIGHT_MARGIN: Int = 28
    private val SCROLL_LINES_PER_NOTCH: Int = 2
    private val FRAME_INTERVAL_MS: Int = 1000 / 60

    private case class Pos(line: Int, col: Int) extends Comparable[Pos]:
        def compareTo(o: Pos): Int =
            val c = Integer.compare(line, o.line)
            if c != 0 then c else Integer.compare(col, o.col)

class NeoEditor(
    val eventBus: EventBus,
    val content: String,
    val editorStatus: NeoEditorStatus,
    val textChangeListener: NeoEditor.TextChangeListener,
) extends JComponent:
    import NeoEditor.*

    private var needsRepaint: Boolean = true
    private var hoveringScrollbar: Boolean = false
    private var hasSelection: Boolean = false
    private var selAnchorX: Int = 0
    private var selAnchorY: Int = 0
    private var draggingSelection: Boolean = false
    private var desiredColumn: Int = -1

    private var currentFont: Font = scala.compiletime.uninitialized
    private var fontColor: Color = scala.compiletime.uninitialized
    private var fm: FontMetrics = scala.compiletime.uninitialized
    private var style: XideStyle = scala.compiletime.uninitialized

    private var cachedLines: ArrayBuffer[String] = scala.compiletime.uninitialized
    private var totalLines: Int = 1
    private var gutterWidth: Int = 40
    private var scrollY: Int = 0

    private var draggingScrollbar: Boolean = false
    private var dragStartY: Int = 0
    private var dragStartScrollY: Int = 0

    setLayout(null)
    setFocusable(true)

    private val pieceTable = new PieceTable(content)
    private val caret = new Caret(eventBus, (x, y, w, h) => needsRepaint = true)
    private val frameTimer: Timer = new Timer(
        FRAME_INTERVAL_MS,
        e => {
            if (needsRepaint) {
                needsRepaint = false
                repaint()
            }
        },
    )
    frameTimer.start()

    style = XideStyle.getCurrent()
    setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR))
    applyTheme()
    eventBus.subscribe(classOf[ThemeChangedEvent], e => applyTheme())
    refreshMetrics()

    private val mouseHandler: MouseAdapter = new MouseAdapter():
        override def mousePressed(e: MouseEvent): Unit = {
            requestFocus()
            val x = e.getX()
            val y = e.getY()

            if (isScrollbarVisible() && isOverScrollbar(x)) {
                beginScrollbarDrag(y)
                return
            }

            val clicked = pointToPosition(x, y)

            if (e.isShiftDown()) {
                beginSelectionIfNeeded()
                caret.moveTo(clicked.col, clicked.line)
                hasSelection = clicked != anchorPos()
            } else {
                clearSelection()
                selAnchorX = clicked.col
                selAnchorY = clicked.line
                caret.moveTo(clicked.col, clicked.line)
                draggingSelection = true
            }

            editorStatus.setCurrentChar(clicked.col + 1)
            editorStatus.setCurrentLine(clicked.line + 1)
            ensureCaretVisible()
            needsRepaint = true
        }

        override def mouseMoved(e: MouseEvent): Unit = {
            val nowHovering = isScrollbarVisible() && isOverScrollbar(e.getX())
            if (nowHovering != hoveringScrollbar) {
                hoveringScrollbar = nowHovering
                needsRepaint = true
            }
        }

        override def mouseDragged(e: MouseEvent): Unit = {
            if (draggingScrollbar) {
                val lineHeight = fm.getHeight()
                val contentHeight = totalLines * lineHeight
                val trackHeight = getHeight()
                val maxScrollY = Math.max(0, contentHeight - trackHeight)

                if (maxScrollY == 0) {
                    return
                }

                val thumbHeight = computeThumbHeight(trackHeight, contentHeight)
                val maxThumbY = Math.max(1, trackHeight - thumbHeight)
                val deltaY = e.getY() - dragStartY

                val deltaScroll = (deltaY.toLong * maxScrollY / maxThumbY).toInt
                setScrollY(dragStartScrollY + deltaScroll)
                return
            }

            if (draggingSelection) {
                val pos = pointToPosition(e.getX(), e.getY())
                caret.moveTo(pos.col, pos.line)
                hasSelection = pos != anchorPos()
                editorStatus.setCurrentChar(pos.col + 1)
                editorStatus.setCurrentLine(pos.line + 1)
                ensureCaretVisible()
                needsRepaint = true
            }
        }

        override def mouseReleased(e: MouseEvent): Unit = {
            draggingScrollbar = false
            draggingSelection = false
        }

        override def mouseWheelMoved(e: MouseWheelEvent): Unit = {
            val lineHeight = fm.getHeight()
            setScrollY(scrollY + e.getWheelRotation() * lineHeight * SCROLL_LINES_PER_NOTCH)
        }

    addMouseListener(mouseHandler)
    addMouseMotionListener(mouseHandler)
    addMouseWheelListener(mouseHandler)

    addComponentListener(new ComponentAdapter():
        override def componentResized(e: ComponentEvent): Unit = {
            clampScrollY()
            needsRepaint = true
        }
    )

    addKeyListener(new KeyAdapter():
        override def keyTyped(e: KeyEvent): Unit = {
            val c = e.getKeyChar()

            if (c == KeyEvent.CHAR_UNDEFINED || Character.isISOControl(c)) {
                return
            }
            if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
                return
            }

            if (hasSelection) {
                deleteSelection()
            }
            insertChar(c)
            textChangeListener.accept()

            ensureCaretVisible()
            needsRepaint = true
        }

        override def keyPressed(e: KeyEvent): Unit = {
            var textChanged = false
            val shift = e.isShiftDown()
            val ctrl = e.isControlDown()

            e.getKeyCode() match {
                case KeyEvent.VK_BACK_SPACE =>
                    if (hasSelection) deleteSelection() else deleteBackward()
                    textChanged = true
                case KeyEvent.VK_DELETE =>
                    if (hasSelection) deleteSelection() else deleteForward()
                    textChanged = true
                case KeyEvent.VK_ENTER =>
                    if (hasSelection) deleteSelection()
                    insertNewline()
                    textChanged = true
                case KeyEvent.VK_LEFT =>
                    if (shift) beginSelectionIfNeeded() else clearSelection()
                    moveCaretLeft()
                    if (shift) hasSelection = true
                case KeyEvent.VK_RIGHT =>
                    if (shift) beginSelectionIfNeeded() else clearSelection()
                    moveCaretRight()
                    if (shift) hasSelection = true
                case KeyEvent.VK_DOWN =>
                    if (shift) beginSelectionIfNeeded() else clearSelection()
                    moveCaretVertical(1)
                    if (shift) hasSelection = true
                case KeyEvent.VK_UP =>
                    if (shift) beginSelectionIfNeeded() else clearSelection()
                    moveCaretVertical(-1)
                    if (shift) hasSelection = true
                case KeyEvent.VK_HOME =>
                    if (shift) beginSelectionIfNeeded() else clearSelection()
                    moveCaretHome()
                    if (shift) hasSelection = true
                case KeyEvent.VK_END =>
                    if (shift) beginSelectionIfNeeded() else clearSelection()
                    moveCaretEnd()
                    if (shift) hasSelection = true
                case KeyEvent.VK_A =>
                    if (ctrl) {
                        selAnchorX = 0
                        selAnchorY = 0
                        val lastLine = totalLines - 1
                        caret.moveTo(cachedLines(lastLine).length(), lastLine)
                        hasSelection = true
                    }
                case KeyEvent.VK_C =>
                    if (ctrl) copySelection()
                case KeyEvent.VK_X =>
                    if (ctrl && hasSelection) {
                        copySelection()
                        deleteSelection()
                        textChanged = true
                    }
                case KeyEvent.VK_V =>
                    if (ctrl) {
                        pasteClipboard()
                        textChanged = true
                    }
                case KeyEvent.VK_SHIFT | KeyEvent.VK_CAPS_LOCK | KeyEvent.VK_F12 |
                    KeyEvent.VK_CONTROL | KeyEvent.VK_ALT => ()
                case _ => ()
            }

            if (textChanged) {
                textChangeListener.accept()
            }

            ensureCaretVisible()
            needsRepaint = true
        }
    )

    private def anchorPos(): Pos = Pos(selAnchorY, selAnchorX)
    private def caretPos(): Pos = Pos(caret.getY(), caret.getX())
    private def selStart(): Pos = if (anchorPos().compareTo(caretPos()) <= 0) anchorPos() else caretPos()
    private def selEnd(): Pos = if (anchorPos().compareTo(caretPos()) <= 0) caretPos() else anchorPos()

    private def pointToPosition(x: Int, y: Int): Pos = {
        val charWidth = fm.charWidth('W')
        val lineHeight = fm.getHeight()

        val adjustedX = x - gutterWidth
        val adjustedY = y + scrollY

        val line = Math.max(0, Math.min(adjustedY / lineHeight, totalLines - 1))
        val rawCh = Math.max(0, (adjustedX + 5) / charWidth)
        val lineLength = if (line < cachedLines.size) cachedLines(line).length() else 0
        val ch = Math.max(0, Math.min(rawCh, lineLength))

        Pos(line, ch)
    }

    private def lineLength(line: Int): Int = {
        if (line >= 0 && line < cachedLines.size) cachedLines(line).length() else 0
    }

    private def moveCaretLeft(): Unit = {
        val x = caret.getX()
        val y = caret.getY()
        if (x > 0) {
            caret.moveTo(x - 1, y)
        } else if (y > 0) {
            caret.moveTo(lineLength(y - 1), y - 1)
        }
        desiredColumn = -1
    }

    private def moveCaretRight(): Unit = {
        val x = caret.getX()
        val y = caret.getY()
        if (x < lineLength(y)) {
            caret.moveTo(x + 1, y)
        } else if (y < totalLines - 1) {
            caret.moveTo(0, y + 1)
        }
        desiredColumn = -1
    }

    private def moveCaretVertical(deltaLine: Int): Unit = {
        val targetLine = Math.max(0, Math.min(totalLines - 1, caret.getY() + deltaLine))
        val column = if (desiredColumn >= 0) desiredColumn else caret.getX()
        desiredColumn = column // preserve across consecutive vertical moves
        val clampedColumn = Math.min(column, lineLength(targetLine))
        caret.moveTo(clampedColumn, targetLine)
    }

    private def moveCaretHome(): Unit = {
        caret.moveTo(0, caret.getY())
        desiredColumn = -1
    }

    private def moveCaretEnd(): Unit = {
        caret.moveTo(lineLength(caret.getY()), caret.getY())
        desiredColumn = -1
    }

    private def getSelectedText(): String = {
        val start = selStart()
        val end = selEnd()
        if (start.line == end.line) {
            return cachedLines(start.line).substring(start.col, end.col)
        }
        val sb = new StringBuilder()
        sb.append(cachedLines(start.line).substring(start.col)).append('\n')
        for (line <- start.line + 1 until end.line) {
            sb.append(cachedLines(line)).append('\n')
        }
        sb.append(cachedLines(end.line), 0, end.col)
        sb.toString()
    }

    private def copySelection(): Unit = {
        if (!hasSelection) return
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(getSelectedText()), null)
    }

    private def pasteClipboard(): Unit = {
        try {
            val text = Toolkit.getDefaultToolkit()
                .getSystemClipboard().getData(DataFlavor.stringFlavor).asInstanceOf[String]
            if (hasSelection) deleteSelection()
            insertText(text)
        } catch {
            case _: Exception =>
        }
    }

    private def insertText(text: String): Unit = {
        for (i <- 0 until text.length()) {
            val c = text.charAt(i)
            if (c == '\n') insertNewline()
            else if (c != '\r') insertChar(c)
        }
    }

    private def charOffsetOf(p: Pos): Int = {
        var offset = 0
        for (i <- 0 until p.line) {
            offset += cachedLines(i).length() + 1
        }
        offset + p.col
    }

    private def deleteSelection(): Unit = {
        if (!hasSelection) return
        val start = selStart()
        val end = selEnd()
        val count = charOffsetOf(end) - charOffsetOf(start)

        caret.moveTo(end.col, end.line)
        for (i <- 0 until count) {
            deleteBackward()
        }
        clearSelection()
    }

    private def beginSelectionIfNeeded(): Unit = {
        if (!hasSelection) {
            selAnchorX = caret.getX()
            selAnchorY = caret.getY()
        }
    }

    private def clearSelection(): Unit = {
        hasSelection = false
    }

    private def applyTheme(): Unit = {
        fontColor = if (style.isDarkTheme()) Color.WHITE else Color.BLACK
        setFont(style.codeFont().deriveFont(FONT_SIZE))
        setBackground(style.shiftAccent(0.3f))
    }

    override def setFont(font: Font): Unit = {
        super.setFont(font)

        this.currentFont = font
        this.fm = getFontMetrics(font)

        caret.setDeltaX(fm.charWidth('W'))
        caret.setDeltaY(fm.getHeight())
        caret.setHeight(fm.getHeight())
    }

    override def getFont(): Font = currentFont

    def getContent: String =
        pieceTable.read().mkString("\n")

    override def paintComponent(g: Graphics): Unit = {
        super.paintComponent(g)

        val g2d = g.asInstanceOf[Graphics2D]
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val width = getWidth()
        val height = getHeight()
        val lineHeight = fm.getHeight()

        g2d.setColor(getBackground())
        g2d.fillRect(0, 0, width, height)

        val firstVisibleLine = Math.max(0, scrollY / lineHeight)
        val visibleLineSlots = height / lineHeight + 2
        val lastVisibleLine = Math.min(totalLines, firstVisibleLine + visibleLineSlots)

        val contentX = gutterWidth
        val contentWidth = Math.max(0, width - gutterWidth - SCROLLBAR_WIDTH)

        val contentG = g2d.create(contentX, 0, contentWidth, height).asInstanceOf[Graphics2D]
        contentG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        contentG.setFont(currentFont)
        contentG.translate(0, -scrollY)

        paintSelection(contentG, firstVisibleLine, lastVisibleLine, lineHeight)
        caret.paintComponent(contentG)

        contentG.setColor(fontColor)
        for (lineIndex <- firstVisibleLine until Math.min(lastVisibleLine, cachedLines.size)) {
            val baselineY = lineHeight * lineIndex + fm.getAscent()
            contentG.drawString(cachedLines(lineIndex), 0, baselineY)
        }
        contentG.dispose()

        paintGutter(g2d, firstVisibleLine, lastVisibleLine, lineHeight)
        paintScrollbar(g2d, width, height, lineHeight)
    }

    private def paintSelection(g: Graphics2D, firstVisibleLine: Int, lastVisibleLine: Int, lineHeight: Int): Unit = {
        if (!hasSelection) return

        val start = selStart()
        val end = selEnd()

        g.setColor(new Color(80, 140, 255, 70))
        val from = Math.max(start.line, firstVisibleLine)
        val to = Math.min(end.line, lastVisibleLine - 1)
        val charWidth = fm.charWidth('W')

        for (line <- from to to) {
            val text = if (line < cachedLines.size) cachedLines(line) else ""
            val colStart = if (line == start.line) start.col else 0
            val colEnd = if (line == end.line) end.col else text.length()

            val x1 = Math.min(colStart, text.length()) * charWidth
            val x2 = Math.min(colEnd, text.length()) * charWidth
            val width = Math.max(x2 - x1, if (colEnd > colStart) 0 else 6)

            g.fillRect(x1, lineHeight * line, Math.max(width, 6), lineHeight)
        }
    }

    private def paintGutter(g2d: Graphics2D, firstVisibleLine: Int, lastVisibleLine: Int, lineHeight: Int): Unit = {
        val base = getBackground()
        val lineNumberColor = new Color(fontColor.getRed(), fontColor.getGreen(), fontColor.getBlue(), 140)

        g2d.setColor(base)
        g2d.fillRect(0, 0, gutterWidth, getHeight())

        g2d.setFont(currentFont)
        g2d.setColor(lineNumberColor)

        for (lineIndex <- firstVisibleLine until lastVisibleLine) {
            val label = String.valueOf(lineIndex + 1)
            val textWidth = fm.stringWidth(label)
            val y = lineHeight * lineIndex - scrollY + fm.getAscent()
            g2d.drawString(label, gutterWidth - textWidth - GUTTER_RIGHT_MARGIN, y)
        }

        g2d.setColor(style.shiftAccent(0.15f))
        g2d.drawLine(gutterWidth - 1, 0, gutterWidth - 1, getHeight())
    }

    private def paintScrollbar(g2d: Graphics2D, width: Int, height: Int, lineHeight: Int): Unit = {
        val contentHeight = Math.max(1, totalLines * lineHeight)
        if (contentHeight <= height) {
            return
        }

        val trackX = width - SCROLLBAR_WIDTH
        g2d.setColor(if (style.isDarkTheme()) new Color(255, 255, 255, 30) else new Color(0, 0, 0, 40))
        g2d.fillRect(trackX, 0, SCROLLBAR_WIDTH, height)

        val thumbHeight = computeThumbHeight(height, contentHeight)
        val maxScrollY = contentHeight - height
        val maxThumbY = height - thumbHeight
        val thumbY = if (maxScrollY <= 0) 0 else (scrollY.toLong * maxThumbY / maxScrollY).toInt

        g2d.setColor(
            if (hoveringScrollbar || draggingScrollbar)
                style.shiftAccent(0.5f)
            else
                style.shiftAccent(0.35f)
        )
        g2d.fillRect(trackX + 2, thumbY, SCROLLBAR_WIDTH - 4, thumbHeight)
    }

    private def computeThumbHeight(trackHeight: Int, contentHeight: Int): Int = {
        Math.max(SCROLLBAR_MIN_THUMB, (trackHeight.toLong * trackHeight / contentHeight).toInt)
    }

    private def isScrollbarVisible(): Boolean = {
        totalLines * fm.getHeight() > getHeight()
    }

    private def isOverScrollbar(x: Int): Boolean = {
        x >= getWidth() - SCROLLBAR_WIDTH
    }

    private def beginScrollbarDrag(clickY: Int): Unit = {
        val lineHeight = fm.getHeight()
        val contentHeight = totalLines * lineHeight
        val trackHeight = getHeight()
        val thumbHeight = computeThumbHeight(trackHeight, contentHeight)
        val maxThumbY = Math.max(1, trackHeight - thumbHeight)
        val maxScrollY = Math.max(0, contentHeight - trackHeight)

        val desiredThumbY = Math.max(0, Math.min(maxThumbY, clickY - thumbHeight / 2))
        setScrollY(if (maxThumbY == 0) 0 else (desiredThumbY.toLong * maxScrollY / maxThumbY).toInt)

        draggingScrollbar = true
        dragStartY = clickY
        dragStartScrollY = scrollY
    }

    private def ensureCaretVisible(): Unit = {
        val lineHeight = fm.getHeight()
        val caretTop = caret.getY() * lineHeight
        val caretBottom = caretTop + lineHeight

        if (caretTop < scrollY) {
            scrollY = caretTop
        } else if (caretBottom > scrollY + getHeight()) {
            scrollY = caretBottom - getHeight()
        }

        clampScrollY()
    }

    private def setScrollY(value: Int): Unit = {
        scrollY = value
        clampScrollY()
        needsRepaint = true
    }

    private def clampScrollY(): Unit = {
        val lineHeight = fm.getHeight()
        val maxScrollY = Math.max(0, totalLines * lineHeight - getHeight())
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY))
    }

    private def refreshMetrics(): Unit = {
        cachedLines = pieceTable.read()
        totalLines = Math.max(1, cachedLines.size)
        updateGutterWidth()
        clampScrollY()
    }

    private def updateGutterWidth(): Unit = {
        val digits = Math.max(2, String.valueOf(totalLines).length())
        gutterWidth = GUTTER_PADDING + digits * fm.charWidth('0') + GUTTER_RIGHT_MARGIN
    }

    private def insertChar(ch: Char): Unit = {
        val x = caret.getX()
        val y = caret.getY()

        pieceTable.insert(String.valueOf(ch), new Position(y, x))

        val line = cachedLines(y)
        cachedLines.update(y, line.substring(0, x) + ch + line.substring(x))

        caret.moveTo(x + 1, y)
    }

    private def insertNewline(): Unit = {
        val x = caret.getX()
        val y = caret.getY()

        pieceTable.insert("\n", new Position(y, x))

        val line = cachedLines(y)
        cachedLines.update(y, line.substring(0, x))
        cachedLines.insert(y + 1, line.substring(x))

        totalLines += 1
        updateGutterWidth()
        clampScrollY()

        caret.moveTo(0, y + 1)
    }

    private def deleteBackward(): Unit = {
        val x = caret.getX()
        val y = caret.getY()

        pieceTable.delete(new Position(y, x))

        if (x > 0) {
            val line = cachedLines(y)
            cachedLines.update(y, line.substring(0, x - 1) + line.substring(x))
            caret.moveTo(x - 1, y)
        } else if (y > 0) {
            val prevLine = cachedLines(y - 1)
            val curLine = cachedLines.remove(y)
            cachedLines.update(y - 1, prevLine + curLine)
            totalLines -= 1
            updateGutterWidth()
            clampScrollY()
            caret.moveTo(prevLine.length(), y - 1)
        }
    }

    private def deleteForward(): Unit = {
        val x = caret.getX()
        val y = caret.getY()

        val line = cachedLines(y)

        if (x < line.length()) {
            pieceTable.delete(new Position(y, x + 1))
            cachedLines.update(y, line.substring(0, x) + line.substring(x + 1))
        } else if (y < cachedLines.size - 1) {
            pieceTable.delete(new Position(y + 1, 0))
            val nextLine = cachedLines.remove(y + 1)
            cachedLines.update(y, line + nextLine)
            totalLines -= 1
            updateGutterWidth()
            clampScrollY()
        }
    }
