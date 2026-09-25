package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.{Cursor, Font, Graphics2D, Rectangle, RenderingHints, Toolkit, Color}
import java.awt.datatransfer.{StringSelection, DataFlavor}

import org.xast.xide.core.utils.Debug
import org.xast.xide.ui.components.code_panel.neo_editor.*

import scala.swing.Component
import scala.swing.event.UIElementResized
   
class NeoEditor(
   val content: String,
   val editorStatus: NeoEditorStatus,
   val onTextChange: () => Unit,
) extends Component:

   private val pieceTable: MutablePieceTable = 
      new MutablePieceTable(content)

   private var metrics: EditorStyleMetrics = 
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
         state = state.mapScroll(_.mapY(EditorLogic.clampScrollY(metrics, pieceTable.lineCount)))
         metrics = metrics.copy(size = this.size)
         repaintFull()
   }

   def getContent: String =
      pieceTable.lines.mkString("\n")

   def dispatch(action: EditorAction): Unit =
      val prev = state
      val (next, effects) = EditorLogic.update(pieceTable, prev, metrics, action)
      state = next

      effects.foreach(runEffect)

      repaintDiff(prev, next)

   def runEffect(effect: Effect): Unit = effect match
      case Effect.CopyToClipboard(text: String) =>
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
      // TODO: make diff for partial repaint
      repaintFull()

   override def paint(g: Graphics2D): Unit = 
      super.paint(g)

      EditorRenderer.paint(g, pieceTable, state, metrics)

   override def font_=(f: Font): Unit = 
      peer.setFont(f)

      metrics = metrics.copy(fontMetrics = peer.getFontMetrics(font))
      state = state |> refreshMetrics

   def refreshMetrics(state: EditorState): EditorState =
      state.copy(
         scroll = state.scroll.mapY(EditorLogic.clampScrollY(metrics, pieceTable.lineCount)),
         gutterWidth = EditorLogic.updatedGutterWidth(metrics, pieceTable.lineCount),
      )

   // TODO: diff

   private def invalidateLines(fromLine: Int, toLine: Int): Unit = {
      val lineHeight = metrics.fontMetrics.getHeight()
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
      if before == after then
         return
            
      (before, after) match {
         case (None, None) => ()
         case (Some((a, b)), None) => invalidateLines(a, b)
         case (None, Some((a, b))) => invalidateLines(a, b)
         case (Some((a1, b1)), Some((a2, b2))) => invalidateLines(Math.min(a1, a2), Math.max(b1, b2))
      }
   }

   private def repaintRect(rect: Rectangle): Unit =
      peer.repaint(rect.x, rect.y, rect.width, rect.height)

   private def repaintFull(): Unit =
      repaint()
