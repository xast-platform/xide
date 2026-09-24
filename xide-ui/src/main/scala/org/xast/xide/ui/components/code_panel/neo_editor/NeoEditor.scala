package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.Cursor
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
import java.awt.Color
import scala.swing.event.UIElementResized
import NeoEditor.*

object NeoEditor:

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

   // FIXME: TEMPORARY PEER
   def component: JComponent = peer

   private val pieceTable = 
      new MutablePieceTable(content)

   private var style: EditorStyle = 
      EditorStyle.initial(this)

   private var state: EditorState = 
      EditorState.initial

   focusable = true
   cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
   refreshMetrics()

   listenTo(
      mouse.clicks, 
      mouse.moves, 
      mouse.wheel, 
      keys, 
      this,
   )

   // Swing adapter reactions (mouse, keyboard)
   reactions += EditorSwingAdapter.toAction.andThen(dispatch)

   // Component-wise reactions
   reactions += {
      case UIElementResized(_) =>
         state = state.mapScroll(_.mapY(clampScrollY))
         invalidateAll()
   }

   private def dispatch(action: EditorAction): Unit =
      val prev = state
      val (next, effects) = EditorLogic.update(pieceTable, prev, style, action)
      state = next

      effects.foreach(runEffect)

   private def runEffect(effect: Effect): Unit =
      {}

   // addKeyListener(new KeyAdapter():
   //    override def keyTyped(e: KeyEvent): Unit = {
   //    }

   //    override def keyPressed(e: KeyEvent): Unit = {
   //       var textChanged = false
   //       val shift = e.isShiftDown()
   //       val ctrl = e.isControlDown()
   //       val prevSelection = selectionLinesOrEmpty()

   //       e.getKeyCode() match {
   //          case KeyEvent.VK_BACK_SPACE =>
   //             if state.selection.nonEmpty then 
   //                deleteSelection() 
   //             else 
   //                deleteBackward()

   //             textChanged = true
   //          case KeyEvent.VK_DELETE =>
   //             if state.selection.nonEmpty then 
   //                deleteSelection() 
   //             else 
   //                deleteForward()

   //             textChanged = true
   //          case KeyEvent.VK_ENTER =>
   //             if state.selection.nonEmpty then 
   //                deleteSelection()

   //             insertNewline()
   //             textChanged = true
   //          case KeyEvent.VK_LEFT =>
   //             if shift then
   //                beginSelectionIfNeeded() 
   //             else 
   //                state = state.mapSelection(_ => Selection.reset(state.caret))

   //             moveCaretLeft()
   //          case KeyEvent.VK_RIGHT =>
   //             if shift then 
   //                beginSelectionIfNeeded() 
   //             else 
   //                state = state.mapSelection(_ => Selection.reset(state.caret))

   //             moveCaretRight()
   //          case KeyEvent.VK_DOWN =>
   //             if shift then 
   //                beginSelectionIfNeeded() 
   //             else 
   //                state = state.mapSelection(_ => Selection.reset(state.caret))

   //             moveCaretVertical(1)
   //          case KeyEvent.VK_UP =>
   //             if shift then 
   //                beginSelectionIfNeeded() 
   //             else 
   //                state = state.mapSelection(_ => Selection.reset(state.caret))

   //             moveCaretVertical(-1)
   //          case KeyEvent.VK_HOME =>
   //             if shift then 
   //                beginSelectionIfNeeded() 
   //             else 
   //                state = state.mapSelection(_ => Selection.reset(state.caret))

   //             moveCaretHome()
   //          case KeyEvent.VK_END =>
   //             if shift then 
   //                beginSelectionIfNeeded() 
   //             else 
   //                state = state.mapSelection(_ => Selection.reset(state.caret))

   //             moveCaretEnd()
   //          case KeyEvent.VK_A =>
   //             if (ctrl) {
   //                state.selection = selection.copy(anchor = Position.zero)
   //                val lastLine = pieceTable.lineCount - 1
   //                state.caret.moveTo(pieceTable.lines(lastLine).length(), lastLine)
   //             }
   //          case KeyEvent.VK_C =>
   //             if ctrl then 
   //                copySelection()
   //          case KeyEvent.VK_X =>
   //             if ctrl && selection.nonEmpty then
   //                copySelection()
   //                deleteSelection()
   //                textChanged = true
   //          case KeyEvent.VK_V =>
   //             if ctrl then
   //                pasteClipboard()
   //                textChanged = true
   //          case KeyEvent.VK_SHIFT | KeyEvent.VK_CAPS_LOCK | KeyEvent.VK_F12 |
   //             KeyEvent.VK_CONTROL | KeyEvent.VK_ALT => ()
   //          case _ => ()
   //       }

   //       if textChanged then
   //          textChangeListener()

   //       ensureCaretVisible()

   //       if (textChanged) {
   //          invalidateAll()
   //       } else {
   //          invalidateSelectionChange(prevSelection)
   //       }
   //    }
   // )

   private def invalidateLines(fromLine: Int, toLine: Int): Unit = {
      val lineHeight = style.fontMetrics.getHeight()
      val top = fromLine * lineHeight - state.scroll.y
      val bottom = (toLine + 1) * lineHeight - state.scroll.y
      invalidateRect(0, top, Math.max(size.width, 1), bottom - top)
   }

   private def selectionLinesOrEmpty(): Option[(Int, Int)] =
      if state.selection.nonEmpty then 
         Some((state.selection.anchor.line, state.selection.active.line)) 
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

      val adjustedX = x - state.gutterWidth
      val adjustedY = y + state.scroll.y

      val line = Math.max(0, Math.min(adjustedY / lineHeight, pieceTable.lineCount - 1))
      val rawCh = Math.max(0, (adjustedX + 5) / charWidth)
      val lineLength = if (line < pieceTable.lines.size) pieceTable.lines(line).length() else 0
      val ch = Math.max(0, Math.min(rawCh, lineLength))

      PiecePos(line, ch)
   }

   private def lineLength(line: Int): Int = {
      if (line >= 0 && line < pieceTable.lines.size) pieceTable.lines(line).length() else 0
   }

   private def moveCaretLeft(): Unit = {
      val x = state.caret.position.col
      val y = state.caret.position.line
      if (x > 0) {
         state = state.mapCaret(_.moveTo(x - 1, y))
      } else if (y > 0) {
         state = state.mapCaret(_.moveTo(lineLength(y - 1), y - 1))
      }
      state = state.mapCaret(_.mapDesiredColumn(_ => None))
   }

   private def moveCaretRight(): Unit = {
      val x = state.caret.position.col
      val y = state.caret.position.line
      if (x < lineLength(y)) {
         state = state.mapCaret(_.moveTo(x + 1, y))
      } else if (y < pieceTable.lineCount - 1) {
         state = state.mapCaret(_.moveTo(0, y + 1))
      }
      state = state.mapCaret(_.mapDesiredColumn(_ => None))
   }

   private def moveCaretVertical(deltaLine: Int): Unit = {
      val targetLine = Math.max(0, Math.min(pieceTable.lineCount - 1, state.caret.position.line + deltaLine))
      val column = 
         if state.caret.checkDesiredColumn(col => col >= 0) then 
            state.caret.desiredColumn.get
         else 
            state.caret.position.col

      val clampedColumn = Math.min(column, lineLength(targetLine))
      state = state.mapCaret(_.copy(
         position = Position(clampedColumn, targetLine),
         desiredColumn = Some(column),
      ))
   }

   private def moveCaretHome(): Unit = {
      state = state.mapCaret(_.copy(
         position = Position(0, state.caret.position.line),
         desiredColumn = None,
      ))
   }

   private def moveCaretEnd(): Unit = {
      state = state.mapCaret(_.copy(
         position = Position(lineLength(state.caret.position.line), state.caret.position.line),
         desiredColumn = None,
      ))
   }

   private def getSelectedText(): String = {
      val start = state.selection.anchor
      val end = state.selection.active
      if (start.line == end.line) {
         return pieceTable.lines(start.line).substring(start.col, end.col)
      }
      var sb = new StringBuilder()
      sb = sb.append(pieceTable.lines(start.line).substring(start.col)).append('\n')
      for (line <- start.line + 1 until end.line) {
         sb = sb.append(pieceTable.lines(line)).append('\n')
      }
      sb = sb.append(pieceTable.lines(end.line).substring(0, end.col))
      sb.toString()
   }

   private def copySelection(): Unit = {
      if state.selection.isEmpty then
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
         if state.selection.nonEmpty then
            state = deleteSelection(state, pieceTable)
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

   private def beginSelectionIfNeeded(): Unit =
      if state.selection.isEmpty then
         state = state.mapSelection(_ => Selection.reset(state.caret))

   private def refreshMetrics(): Unit = {
      state = state.copy(
         scroll = state.scroll.mapY(clampScrollY),
         gutterWidth = updatedGutterWidth(),
      )
   }

   override def font_=(f: Font): Unit = 
      peer.setFont(f)

      style = style.copy(fontMetrics = peer.getFontMetrics(font))

      // state.caret.setDeltaX(style.fontMetrics.charWidth('W'))
      // state.caret.setDeltaY(style.fontMetrics.getHeight())
      // state.caret.setHeight(style.fontMetrics.getHeight())

   def getContent: String =
      pieceTable.lines.mkString("\n")

   override def paint(g: Graphics2D): Unit = 
      super.paint(g)

      val g2d = g.asInstanceOf[Graphics2D]
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      val width = size.width
      val height = size.height
      val lineHeight = style.fontMetrics.getHeight()

      g2d.setColor(background)
      g2d.fillRect(0, 0, width, height)

      val firstVisibleLine = Math.max(0, state.scroll.y / lineHeight)
      val visibleLineSlots = height / lineHeight + 2
      val lastVisibleLine = Math.min(pieceTable.lineCount, firstVisibleLine + visibleLineSlots)

      val contentX = state.gutterWidth
      val contentWidth = Math.max(0, width - state.gutterWidth - SCROLLBAR_WIDTH)

      val contentG = g2d.create(contentX, 0, contentWidth, height).asInstanceOf[Graphics2D]
      contentG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      contentG.setFont(font)
      contentG.translate(0, -state.scroll.y)

      paintSelection(contentG, firstVisibleLine, lastVisibleLine, lineHeight)
      // state.caret.paintComponent(contentG)

      contentG.setColor(style.fontColor)
      for (lineIndex <- firstVisibleLine until Math.min(lastVisibleLine, pieceTable.lines.size)) {
         val baselineY = lineHeight * lineIndex + style.fontMetrics.getAscent()
         contentG.drawString(pieceTable.lines(lineIndex), 0, baselineY)
      }
      contentG.dispose()

      paintGutter(g2d, firstVisibleLine, lastVisibleLine, lineHeight)
      paintScrollbar(g2d, width, height, lineHeight)

   private def paintSelection(g: Graphics2D, firstVisibleLine: Int, lastVisibleLine: Int, lineHeight: Int): Unit = {
      if state.selection.isEmpty then 
         return

      val start = state.selection.anchor
      val end = state.selection.active

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
      val base = background
      val lineNumberColor = new Color(style.fontColor.getRed(), style.fontColor.getGreen(), style.fontColor.getBlue(), 140)

      val width = size.width
      val height = size.height

      g2d.setColor(base)
      g2d.fillRect(0, 0, state.gutterWidth, height)

      g2d.setFont(font)
      g2d.setColor(lineNumberColor)

      for (lineIndex <- firstVisibleLine until lastVisibleLine) {
         val label = String.valueOf(lineIndex + 1)
         val textWidth = style.fontMetrics.stringWidth(label)
         val y = lineHeight * lineIndex - state.scroll.y + style.fontMetrics.getAscent()
         g2d.drawString(label, state.gutterWidth - textWidth - GUTTER_RIGHT_MARGIN, y)
      }

      g2d.setColor(style.xideStyle.shiftAccent(0.15f))
      g2d.drawLine(state.gutterWidth - 1, 0, state.gutterWidth - 1, height)
   }

   private def paintScrollbar(g2d: Graphics2D, width: Int, height: Int, lineHeight: Int): Unit = {
      val contentHeight = Math.max(1, pieceTable.lineCount * lineHeight)
      if (contentHeight <= height) {
         return
      }

      val trackX = width - SCROLLBAR_WIDTH
      g2d.setColor(
         if style.xideStyle.isDarkTheme then 
            new Color(255, 255, 255, 30) 
         else 
            new Color(0, 0, 0, 40)
      )
      g2d.fillRect(trackX, 0, SCROLLBAR_WIDTH, height)

      val thumbHeight = computeThumbHeight(height, contentHeight)
      val maxScrollY = contentHeight - height
      val maxThumbY = height - thumbHeight
      val thumbY = if (maxScrollY <= 0) 0 else (state.scroll.y.toLong * maxThumbY / maxScrollY).toInt

      g2d.setColor(
         if (state.hoveringScrollbar || state.draggingScrollbar)
            style.xideStyle.shiftAccent(0.5f)
         else
            style.xideStyle.shiftAccent(0.35f)
      )
      g2d.fillRect(trackX + 2, thumbY, SCROLLBAR_WIDTH - 4, thumbHeight)
   }

   private def computeThumbHeight(trackHeight: Int, contentHeight: Int): Int = {
      Math.max(SCROLLBAR_MIN_THUMB, (trackHeight.toLong * trackHeight / contentHeight).toInt)
   }

   def scrollbarVisible: Boolean =
      pieceTable.lineCount * style.fontMetrics.getHeight() > size.height

   def isOverScrollbar(x: Int): Boolean = 
      x >= size.width - SCROLLBAR_WIDTH

   private def beginScrollbarDrag(clickY: Int): Unit = {
      val lineHeight = style.fontMetrics.getHeight()
      val contentHeight = pieceTable.lineCount * lineHeight
      val trackHeight = size.height
      val thumbHeight = computeThumbHeight(trackHeight, contentHeight)
      val maxThumbY = Math.max(1, trackHeight - thumbHeight)
      val maxScrollY = Math.max(0, contentHeight - trackHeight)
      val desiredThumbY = Math.max(0, Math.min(maxThumbY, clickY - thumbHeight / 2))
      setScrollY(if (maxThumbY == 0) 0 else (desiredThumbY.toLong * maxScrollY / maxThumbY).toInt)

      state = state.copy(
         draggingScrollbar = true,
         scroll = state.scroll.copy(
            dragStartY = clickY, 
            dragStartScrollY = state.scroll.y,
         ),
      )
   }

   private def setScrollY(value: Int): Unit = {
      state = state.mapScroll(_.mapY(clampScrollY))
      invalidateAll()
   }

   private def updatedGutterWidth(): Int =
      val digits = Math.max(2, String.valueOf(pieceTable.lineCount).length())
      GUTTER_PADDING + digits * style.fontMetrics.charWidth('0') + GUTTER_RIGHT_MARGIN

   private def invalidateRect(x: Int, y: Int, w: Int, h: Int): Unit =
      peer.repaint(x, y, w, h)

   private def invalidateAll(): Unit =
      repaint()


   private def insertNewline(): Unit = {
      val x = state.caret.position.col
      val y = state.caret.position.line

      pieceTable.insert("\n", new PiecePos(y, x))

      state = state.copy(
         scroll = state.scroll.mapY(clampScrollY),
         caret = state.caret.moveTo(0, y + 1),
         gutterWidth = updatedGutterWidth(),
      )
   }

   private def deleteBackward(): Unit = {
      val x = state.caret.position.col
      val y = state.caret.position.line
      val prevLineLengthBeforeMerge = if (y > 0) pieceTable.lines(y - 1).length() else 0

      pieceTable.delete(new PiecePos(y, x))

      if (x > 0) {
         state = state.mapCaret(_.moveTo(x - 1, y))
      } else if (y > 0) {
         state = state.copy(
            gutterWidth = updatedGutterWidth(),
            scroll = state.scroll.mapY(clampScrollY),
            caret = state.caret.moveTo(prevLineLengthBeforeMerge, y - 1),
         )
      }
   }

   private def deleteForward(): Unit = {
      val x = state.caret.position.col
      val y = state.caret.position.line
      val lineLength = pieceTable.lines(y).length()
      val lastLineIndex = pieceTable.lines.size - 1

      if (x < lineLength) {
         pieceTable.delete(new PiecePos(y, x + 1))
      } else if (y < lastLineIndex) {
         pieceTable.delete(new PiecePos(y + 1, 0))
         state = state.copy(
            scroll = state.scroll.mapY(clampScrollY),
            gutterWidth = updatedGutterWidth(),
         )
      }
   }
