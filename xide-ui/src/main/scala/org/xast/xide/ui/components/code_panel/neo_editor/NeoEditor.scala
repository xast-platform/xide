package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.Cursor as CursorComponent
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
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
import org.xast.xide.core.utils.Debug
import org.xast.xide.ui.components.code_panel.neo_editor.*
import org.xast.xide.ui.utils.XideStyle
import scala.collection.mutable.ArrayBuffer
import scala.swing.Component
import scala.swing.Color

object NeoEditor:

   private val FONT_SIZE: Float = 20f
   private val SCROLLBAR_WIDTH: Int = 16
   private val SCROLLBAR_MIN_THUMB: Int = 20
   private val GUTTER_PADDING: Int = 32
   private val GUTTER_RIGHT_MARGIN: Int = 28
   private val SCROLL_LINES_PER_NOTCH: Int = 2
   
class NeoEditor(
   val eventBus: EventBus,
   val content: String,
   val editorStatus: NeoEditorStatus,
   val textChangeListener: () => Unit,
) extends Component:

   import NeoEditor.*

   private val pieceTable = new PieceTable(content)
   private var selection: Selection = Selection.zero
   private var cursor: Cursor = Cursor.zero
   private var style: EditorStyle = createStyle
   // private var cursor: Cursor = 

   

   private var hoveringScrollbar: Boolean = false
   private var draggingSelection: Boolean = false

   private var totalLines: Int = 1
   private var gutterWidth: Int = 40
   private var scrollY: Int = 0

   private var draggingScrollbar: Boolean = false
   private var dragStartY: Int = 0
   private var dragStartScrollY: Int = 0


   private def createStyle: EditorStyle =
      val xideStyle = XideStyle.getCurrent()
      val fontColor = if xideStyle.isDarkTheme then
         Color.WHITE 
      else 
         Color.BLACK

      val font = xideStyle.codeFont.deriveFont(FONT_SIZE)
      val fontMetrics = peer.getFontMetrics(font)
      val bgColor = xideStyle.shiftAccent(0.3f)

      EditorStyle(
         fontMetrics,
         fontColor,
         bgColor,
         xideStyle,
      )

   focusable = true

   private val caret = new Caret(eventBus, (x, y, w, h) => invalidateRect(gutterWidth + x, y - scrollY, w, h))

   style = XideStyle.getCurrent()
   cursor = CursorComponent.getPredefinedCursor(CursorComponent.TEXT_CURSOR)
   applyTheme()
   eventBus.subscribe(classOf[ThemeChangedEvent], _ => applyTheme())
   refreshMetrics()

   addMouseListener(mouseHandler)
   addMouseMotionListener(mouseHandler)
   addMouseWheelListener(mouseHandler)

   addComponentListener(new ComponentAdapter():
      override def componentResized(e: ComponentEvent): Unit = {
         clampScrollY()
         invalidateAll()
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

         if selection.isDefined then {
            deleteSelection()
         }
         insertChar(c)
         textChangeListener()

         ensureCaretVisible()
         invalidateAll()
      }

      override def keyPressed(e: KeyEvent): Unit = {
         var textChanged = false
         val shift = e.isShiftDown()
         val ctrl = e.isControlDown()
         val prevSelection = selectionLinesOrEmpty()

         e.getKeyCode() match {
            case KeyEvent.VK_BACK_SPACE =>
               if selection.isDefined then deleteSelection() else deleteBackward()
               textChanged = true
            case KeyEvent.VK_DELETE =>
               if selection.isDefined then deleteSelection() else deleteForward()
               textChanged = true
            case KeyEvent.VK_ENTER =>
               if selection.isDefined then deleteSelection()
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
                  caret.moveTo(pieceTable.lines(lastLine).length(), lastLine)
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

         if (textChanged) {
            invalidateAll()
         } else {
            invalidateSelectionChange(prevSelection)
         }
      }
   )

   private def anchorPos(): Position = Position(selAnchorY, selAnchorX)
   private def caretPos(): Position = Position(caret.getY(), caret.getX())
   private def selStart(): Position = if (anchorPos().compareTo(caretPos()) <= 0) anchorPos() else caretPos()
   private def selEnd(): Position = if (anchorPos().compareTo(caretPos()) <= 0) caretPos() else anchorPos()

   private def invalidateLines(fromLine: Int, toLine: Int): Unit = {
      val lineHeight = style.fontMetrics.getHeight()
      val top = fromLine * lineHeight - scrollY
      val bottom = (toLine + 1) * lineHeight - scrollY
      invalidateRect(0, top, Math.max(getWidth(), 1), bottom - top)
   }

   private def selectionLinesOrEmpty(): Option[(Int, Int)] =
      if selection.isDefined then 
         Some((selStart().line, selEnd().line)) 
      else 
         None

   private def invalidateSelectionChange(before: Option[(Int, Int)]): Unit = {
      val after = selectionLinesOrEmpty()
      if (before == after) {
         return
      }
      (before, after) match {
         case (None, None) => ()
         case (Some((a, b)), None) => invalidateLines(a, b)
         case (None, Some((a, b))) => invalidateLines(a, b)
         case (Some((a1, b1)), Some((a2, b2))) => invalidateLines(Math.min(a1, a2), Math.max(b1, b2))
      }
   }

   private def pointToPiecePos(x: Int, y: Int): PiecePos = {
      val charWidth = style.fontMetrics.charWidth('W')
      val lineHeight = style.fontMetrics.getHeight()

      val adjustedX = x - gutterWidth
      val adjustedY = y + scrollY

      val line = Math.max(0, Math.min(adjustedY / lineHeight, totalLines - 1))
      val rawCh = Math.max(0, (adjustedX + 5) / charWidth)
      val lineLength = if (line < pieceTable.lines.size) pieceTable.lines(line).length() else 0
      val ch = Math.max(0, Math.min(rawCh, lineLength))

      PiecePos(line, ch)
   }

   private def lineLength(line: Int): Int = {
      if (line >= 0 && line < pieceTable.lines.size) pieceTable.lines(line).length() else 0
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
         return pieceTable.lines(start.line).substring(start.col, end.col)
      }
      val sb = new StringBuilder()
      sb.append(pieceTable.lines(start.line).substring(start.col)).append('\n')
      for (line <- start.line + 1 until end.line) {
         sb.append(pieceTable.lines(line)).append('\n')
      }
      sb.append(pieceTable.lines(end.line).substring(0, end.col))
      sb.toString()
   }

   private def copySelection(): Unit = {
      if selection.isEmpty then
         return
      try {
         Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(getSelectedText()), null)
      } catch {
         case e: Exception =>
            Debug.error("copySelection failed: " + e)
      }
   }

   private def pasteClipboard(): Unit = {
      try {
         val text = Toolkit.getDefaultToolkit()
            .getSystemClipboard().getData(DataFlavor.stringFlavor).asInstanceOf[String]
         if selection.isDefined then
            deleteSelection()
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

   private def deleteSelection(): Unit = {
      if selection.isEmpty then 
         return
      val start = selStart()
      val end = selEnd()

      pieceTable.deleteRange(new PiecePos(start.line, start.col), new PiecePos(end.line, end.col))
      caret.moveTo(start.col, start.line)
      clearSelection()
   }

   private def beginSelectionIfNeeded(): Unit =
      if selection.isEmpty then
         selection = Some(Selection(
            Position(caret.getX(), caret.getY()),
            Position.zero,
         ))

   private def clearSelection(): Unit =
      selection = Selection.reset(cursor)

   private def refreshMetrics(): Unit = {
      totalLines = Math.max(1, pieceTable.lines.size)
      updateGutterWidth()
      clampScrollY()
   }

   override def font_=(f: Font): Unit = 
      peer.setFont(f)

      style = style.copy(fontMetrics = peer.getFontMetrics(font))

      caret.setDeltaX(style.fontMetrics.charWidth('W'))
      caret.setDeltaY(style.fontMetrics.getHeight())
      caret.setHeight(style.fontMetrics.getHeight())

   def getContent: String =
      pieceTable.lines.mkString("\n")

   override def paintComponent(g: Graphics): Unit = {
      super.paintComponent(g)

      val g2d = g.asInstanceOf[Graphics2D]
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      val width = getWidth()
      val height = getHeight()
      val lineHeight = style.fontMetrics.getHeight()

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
      for (lineIndex <- firstVisibleLine until Math.min(lastVisibleLine, pieceTable.lines.size)) {
         val baselineY = lineHeight * lineIndex + style.fontMetrics.getAscent()
         contentG.drawString(pieceTable.lines(lineIndex), 0, baselineY)
      }
      contentG.dispose()

      paintGutter(g2d, firstVisibleLine, lastVisibleLine, lineHeight)
      paintScrollbar(g2d, width, height, lineHeight)
   }

   private def paintSelection(g: Graphics2D, firstVisibleLine: Int, lastVisibleLine: Int, lineHeight: Int): Unit = {
      if selection.isEmpty then 
         return

      val start = selStart()
      val end = selEnd()

      g.setColor(new Color(80, 140, 255, 70))
      val from = Math.max(start.line, firstVisibleLine)
      val to = Math.min(end.line, lastVisibleLine - 1)
      val charWidth = style.fontMetrics.charWidth('W')

      for (line <- from to to) {
         val text = if (line < pieceTable.lines.size) pieceTable.lines(line) else ""
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
         val textWidth = style.fontMetrics.stringWidth(label)
         val y = lineHeight * lineIndex - scrollY + style.fontMetrics.getAscent()
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

   def scrollbarVisible: Boolean =
      totalLines * style.fontMetrics.getHeight() > getHeight()

   def isOverScrollbar(x: Int): Boolean = 
      x >= getWidth() - SCROLLBAR_WIDTH

   private def beginScrollbarDrag(clickY: Int): Unit = {
      val lineHeight = style.fontMetrics.getHeight()
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
      val lineHeight = style.fontMetrics.getHeight()
      val caretTop = caret.getY() * lineHeight
      val caretBottom = caretTop + lineHeight
      val previousScrollY = scrollY

      if (caretTop < scrollY) {
         scrollY = caretTop
      } else if (caretBottom > scrollY + getHeight()) {
         scrollY = caretBottom - getHeight()
      }

      clampScrollY()

      if (scrollY != previousScrollY) {
         invalidateAll()
      }
   }

   private def setScrollY(value: Int): Unit = {
      scrollY = value
      clampScrollY()
      invalidateAll()
   }

   private def clampScrollY(): Unit = {
      val lineHeight = style.fontMetrics.getHeight()
      val maxScrollY = Math.max(0, totalLines * lineHeight - getHeight())
      scrollY = Math.max(0, Math.min(scrollY, maxScrollY))
   }

   private def updateGutterWidth(): Unit = {
      val digits = Math.max(2, String.valueOf(totalLines).length())
      gutterWidth = GUTTER_PADDING + digits * style.fontMetrics.charWidth('0') + GUTTER_RIGHT_MARGIN
   }

   private def insertChar(ch: Char): Unit = {
      val x = caret.getX()
      val y = caret.getY()

      pieceTable.insert(String.valueOf(ch), new PiecePos(y, x))

      caret.moveTo(x + 1, y)
   }

   private def invalidateRect(x: Int, y: Int, w: Int, h: Int): Unit =
      peer.repaint(x, y, w, h)

   private def invalidateAll(): Unit =
      repaint()


   private def insertNewline(): Unit = {
      val x = caret.getX()
      val y = caret.getY()

      pieceTable.insert("\n", new PiecePos(y, x))

      totalLines += 1
      updateGutterWidth()
      clampScrollY()

      caret.moveTo(0, y + 1)
   }

   private def deleteBackward(): Unit = {
      val x = caret.getX()
      val y = caret.getY()
      val prevLineLengthBeforeMerge = if (y > 0) pieceTable.lines(y - 1).length() else 0

      pieceTable.delete(new PiecePos(y, x))

      if (x > 0) {
         caret.moveTo(x - 1, y)
      } else if (y > 0) {
         totalLines -= 1
         updateGutterWidth()
         clampScrollY()
         caret.moveTo(prevLineLengthBeforeMerge, y - 1)
      }
   }

   private def deleteForward(): Unit = {
      val x = caret.getX()
      val y = caret.getY()
      val lineLength = pieceTable.lines(y).length()
      val lastLineIndex = pieceTable.lines.size - 1

      if (x < lineLength) {
         pieceTable.delete(new PiecePos(y, x + 1))
      } else if (y < lastLineIndex) {
         pieceTable.delete(new PiecePos(y + 1, 0))
         totalLines -= 1
         updateGutterWidth()
         clampScrollY()
      }
   }
