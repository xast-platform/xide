package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.{Cursor, Font, Graphics2D, Rectangle, Toolkit}
import java.awt.datatransfer.{StringSelection, DataFlavor}
import javax.swing.Timer

import org.xast.xide.core.utils.Debug
import org.xast.xide.ui.components.code_panel.neo_editor.*

import scala.swing.Component
import scala.swing.event.UIElementResized
   
class EditorComponent(
   val content: String,
   val editorStatus: EditorStatus,
   val onTextChange: () => Unit,
) extends Component:

   private val pieceTable: MutablePieceTable = 
      new MutablePieceTable(content)

   private var metrics: EditorStyleMetrics = 
      EditorStyleMetrics.initial(this)

   private var model: EditorModel = EditorModel(
      state = EditorState.initial |> refreshMetrics,
      history = EditorHistory(),
   )

   private val caretBlinkTimer: Timer = 
      new Timer(EditorStyleMetrics.caretBlinkIntervalMs, _ => dispatch(EditorAction.BlinkCaret))

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
         model = model.copy(state = model.state.mapScroll(_.mapY(EditorLogic.clampScrollY(metrics, pieceTable.lineCount))))
         metrics = metrics.copy(size = this.size)
         repaintFull()
   }

   caretBlinkTimer.setInitialDelay(EditorStyleMetrics.caretBlinkIntervalMs)
   caretBlinkTimer.start()

   def getContent: String =
      pieceTable.lines.mkString("\n")

   def dispatch(action: EditorAction): Unit =
      val prev = model
      val prevLineCount = pieceTable.lineCount
      val (next, effects) = EditorLogic.update(pieceTable, prev, metrics, action, System.currentTimeMillis())
      model = next

      if effects.contains(Effect.RepaintAll) then
         repaintFull()
      else
         repaintDiff(prev.state, next.state, prevLineCount, effects.contains(Effect.TextChanged))

      effects.foreach(runEffect)

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

      case Effect.RestartCaretBlink => caretBlinkTimer.restart()

      case Effect.RepaintAll => () // handled in dispatch

   def repaintDiff(
      prev: EditorState,
      next: EditorState,
      prevLineCount: Int,
      textChanged: Boolean,
   ): Unit = EditorRepaint.diff(
         prev, 
         next, 
         metrics, 
         prevLineCount, 
         pieceTable.lineCount, 
         textChanged
      ) match
         case Some(RepaintAmount.All) => 
            repaintFull()

         case Some(RepaintAmount.Rects(rects)) => 
            rects.foreach(repaintRect)

         case _ => {}

   def refreshMetrics(state: EditorState): EditorState =
      state.copy(
         scroll = state.scroll.mapY(EditorLogic.clampScrollY(metrics, pieceTable.lineCount)),
         gutterWidth = EditorLogic.updatedGutterWidth(metrics, pieceTable.lineCount),
      )

   def repaintRect(rect: Rectangle): Unit =
      peer.repaint(rect.x, rect.y, rect.width, rect.height)

   def repaintFull(): Unit =
      repaint()

   override def paint(g: Graphics2D): Unit = 
      super.paint(g)

      EditorRenderer.paint(g, pieceTable, model.state, metrics)

   override def font_=(f: Font): Unit = 
      peer.setFont(f)

      metrics = metrics.copy(fontMetrics = peer.getFontMetrics(font))
      model = model.copy(state = model.state |> refreshMetrics)
