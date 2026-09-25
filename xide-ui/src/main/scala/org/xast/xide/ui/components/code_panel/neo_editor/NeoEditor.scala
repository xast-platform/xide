package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.Cursor
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.Color

import javax.swing.JComponent

import org.xast.xide.core.event.EventBus
import org.xast.xide.core.utils.Debug
import org.xast.xide.ui.components.code_panel.neo_editor.*

import scala.swing.Component
import scala.swing.event.UIElementResized

import java.awt.datatransfer.DataFlavor
import org.xast.xide.ui.components.code_panel.neo_editor.Effect.UpdateEditorStatus
   
class NeoEditor(
   val eventBus: EventBus,
   val content: String,
   val editorStatus: NeoEditorStatus,
   val onTextChange: () => Unit,
) extends Component:

   // FIXME: TEMPORARY PEER
   def component: JComponent = peer

   private val pieceTable = 
      new MutablePieceTable(content)

   private var style: EditorStyleMetrics = 
      EditorStyleMetrics.initial(this)

   private var state: EditorState = 
      EditorState.initial |> refreshMetrics

   focusable = true
   cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)

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
         state = state.mapScroll(_.mapY(EditorLogic.clampScrollY(style, pieceTable.lineCount)))
         style = style.copy(size = this.size)
         repaintFull()
   }

   def dispatch(action: EditorAction): Unit =
      val prev = state
      val (next, effects) = EditorLogic.update(pieceTable, prev, style, action)
      state = next

      effects.foreach(runEffect)

      repaintDiff(prev, next)

   def runEffect(effect: Effect): Unit = effect match
      case Effect.CopyToClipboard(text: String) =>
         if state.selection.isEmpty then
            return

         try
            Toolkit.getDefaultToolkit()
               .getSystemClipboard()
               .setContents(new StringSelection(text), null)
         catch
            case e: Exception =>
               Debug.error("Selection copy failed: " + e)

      case Effect.RequestPaste => 
         try
            val text = Toolkit.getDefaultToolkit()
               .getSystemClipboard()
               .getData(DataFlavor.stringFlavor)
               .asInstanceOf[String]

            dispatch(EditorAction.Paste(text))
         catch
            case e: Exception =>
               Debug.error("Paste failed: " + e)

      case Effect.TextChanged => onTextChange()

      case Effect.UpdateEditorStatus(currentChar, currentLine) =>
         editorStatus.setCurrentChar(currentChar)
         editorStatus.setCurrentLine(currentLine)

      case Effect.RequestFocus => requestFocus()

   def repaintDiff(
      prev: EditorState,
      next: EditorState,
   ): Unit =
      {}

      // case Effect.RepaintCanvas(RepaintAmount.All) => 
      //    repaint()

      // case Effect.RepaintCanvas(RepaintAmount.Rect(rect)) =>
      //    repaintRect(rect)

   // private val mouseHandler: MouseAdapter = new MouseAdapter():
      // override def mouseMoved(e: MouseEvent): Unit = {
      //    val nowHovering = isScrollbarVisible() && isOverScrollbar(e.getX())
      //    if (nowHovering != hoveringScrollbar) {
      //       hoveringScrollbar = nowHovering
      //       needsRepaint = true
      //    }
      // }

      // override def mouseDragged(e: MouseEvent): Unit = {
      //    if (draggingScrollbar) {
      //       val lineHeight = fm.getHeight()
      //       val contentHeight = totalLines * lineHeight
      //       val trackHeight = getHeight()
      //       val maxScrollY = Math.max(0, contentHeight - trackHeight)

      //       if (maxScrollY == 0) {
      //          return
      //       }

      //       val thumbHeight = computeThumbHeight(trackHeight, contentHeight)
      //       val maxThumbY = Math.max(1, trackHeight - thumbHeight)
      //       val deltaY = e.getY() - dragStartY

      //       val deltaScroll = (deltaY.toLong * maxScrollY / maxThumbY).toInt
      //       setScrollY(dragStartScrollY + deltaScroll)
      //       return
      //    }

      //    if (draggingSelection) {
      //       val prevSelection = selectionLinesOrEmpty()
      //       val pos = pointToPiecePos(e.getX(), e.getY())
      //       caret.moveTo(pos.col, pos.line)
      //       hasSelection = pos != anchorPos()
      //       editorStatus.setCurrentChar(pos.col + 1)
      //       editorStatus.setCurrentLine(pos.line + 1)
      //       ensureCaretVisible()
      //       invalidateSelectionChange(prevSelection)
      //    }
      // }

      // override def mouseReleased(e: MouseEvent): Unit = {
      //    draggingScrollbar = false
      //    draggingSelection = false
      // }

      // override def mouseWheelMoved(e: MouseWheelEvent): Unit = {
      //    val lineHeight = fm.getHeight()
      //    setScrollY(scrollY + e.getWheelRotation() * lineHeight * scrollLinesPerNotch)
      // }

   private def invalidateLines(fromLine: Int, toLine: Int): Unit = {
      val lineHeight = style.fontMetrics.getHeight()
      val top = fromLine * lineHeight - state.scroll.y
      val bottom = (toLine + 1) * lineHeight - state.scroll.y
      repaintRect(new Rectangle(0, top, Math.max(size.width, 1), bottom - top))
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

   private def refreshMetrics(state: EditorState): EditorState =
      state.copy(
         scroll = state.scroll.mapY(EditorLogic.clampScrollY(style, pieceTable.lineCount)),
         gutterWidth = EditorLogic.updatedGutterWidth(style, pieceTable.lineCount),
      )

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
      val contentWidth = Math.max(0, width - state.gutterWidth - EditorStyleMetrics.scrollbarWidth)

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
         g2d.drawString(label, state.gutterWidth - textWidth - EditorStyleMetrics.gutterRightMargin, y)
      }

      g2d.setColor(style.xideStyle.shiftAccent(0.15f))
      g2d.drawLine(state.gutterWidth - 1, 0, state.gutterWidth - 1, height)
   }

   private def paintScrollbar(g2d: Graphics2D, width: Int, height: Int, lineHeight: Int): Unit = {
      val contentHeight = Math.max(1, pieceTable.lineCount * lineHeight)
      if (contentHeight <= height) {
         return
      }

      val trackX = width - EditorStyleMetrics.scrollbarWidth
      g2d.setColor(
         if style.xideStyle.isDarkTheme then 
            new Color(255, 255, 255, 30) 
         else 
            new Color(0, 0, 0, 40)
      )
      g2d.fillRect(trackX, 0, EditorStyleMetrics.scrollbarWidth, height)

      val thumbHeight = EditorStyleMetrics.computeThumbHeight(height, contentHeight)
      val maxScrollY = contentHeight - height
      val maxThumbY = height - thumbHeight
      val thumbY = if (maxScrollY <= 0) 0 else (state.scroll.y.toLong * maxThumbY / maxScrollY).toInt

      g2d.setColor(
         if (state.hoveringScrollbar || state.draggingScrollbar)
            style.xideStyle.shiftAccent(0.5f)
         else
            style.xideStyle.shiftAccent(0.35f)
      )
      g2d.fillRect(trackX + 2, thumbY, EditorStyleMetrics.scrollbarWidth - 4, thumbHeight)
   }

   private def repaintRect(rect: Rectangle): Unit =
      peer.repaint(rect.x, rect.y, rect.width, rect.height)

   private def repaintFull(): Unit =
      repaint()
