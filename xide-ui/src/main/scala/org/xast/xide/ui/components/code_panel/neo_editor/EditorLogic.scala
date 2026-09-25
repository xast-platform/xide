package org.xast.xide.ui.components.code_panel.neo_editor

import scala.swing.Rectangle
import org.xast.xide.ui.components.code_panel.neo_editor.EditorAction.*

// FIXME: move to utils after scala rewrite
extension [A](value: A)
   infix def |>[B](f: A => B): B = f(value)

def when[A](pred: A => Boolean, f: A => A)(value: A): A =
   if pred(value) then f(value) else value

def whenElse[A](pred: A => Boolean, f: A => A, elseF: A => A)(value: A): A =
   if pred(value) then f(value) else elseF(value)

def unless[A](pred: A => Boolean, f: A => A)(value: A): A =
   if !pred(value) then f(value) else value

type Effectful[T] = (T, List[Effect])

object Effectful:

   def pure[T](value: T): Effectful[T] = (value, List.empty)

enum Effect:
   case CopyToClipboard(text: String)
   case RequestPaste
   case TextChanged
   case UpdateEditorStatus(currentChar: Int, currentLine: Int)
   case RequestFocus

enum RepaintAmount:
   case All
   case Rect(rect: Rectangle)

object EditorLogic:

   def update(
      pieceTable: MutablePieceTable,
      state: EditorState,
      metrics: EditorStyleMetrics,
      action: EditorAction,
   ): Effectful[EditorState] = action match
      case TypeChar(c) =>
         val newState = state
            |> when(_.selection.nonEmpty, deleteSelection(pieceTable))
            |> insertChar(pieceTable, c)
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         (newState, List(Effect.TextChanged))

      case Backspace => 
         val newState = state
            |> whenElse(_.selection.nonEmpty, deleteSelection(pieceTable), deleteBackward(metrics, pieceTable))
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         (newState, List(Effect.TextChanged))

      case Delete =>
         val newState = state
            |> whenElse(_.selection.nonEmpty, deleteSelection(pieceTable), deleteForward(metrics, pieceTable))
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         (newState, List(Effect.TextChanged))

      case Enter =>
         val newState = state
            |> when(_.selection.nonEmpty, deleteSelection(pieceTable))
            |> insertNewline(pieceTable, metrics)
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         (newState, List(Effect.TextChanged))
      
      case MoveLeft(shift) => 
         val newState = state
            |> whenElse(_ => shift, beginSelectionIfNeeded, resetSelection)
            |> moveCaretLeft(pieceTable)
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case MoveRight(shift) => 
         val newState = state
            |> whenElse(_ => shift, beginSelectionIfNeeded, resetSelection)
            |> moveCaretRight(pieceTable)
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case MoveDown(shift) =>
         val newState = state
            |> whenElse(_ => shift, beginSelectionIfNeeded, resetSelection)
            |> moveCaretVertical(pieceTable, 1)
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case MoveUp(shift) =>
         val newState = state
            |> whenElse(_ => shift, beginSelectionIfNeeded, resetSelection)
            |> moveCaretVertical(pieceTable, -1)
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case PressHome(shift) =>
         val newState = state
            |> whenElse(_ => shift, beginSelectionIfNeeded, resetSelection)
            |> moveCaretHome
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case PressEnd(shift) =>
         val newState = state
            |> whenElse(_ => shift, beginSelectionIfNeeded, resetSelection)
            |> moveCaretEnd(pieceTable)
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case SelectAll =>
         val newState = state
            |> selectAllLines(pieceTable)
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case Copy => 
         val text = state |> getSelectedText(pieceTable)

         (state, List(Effect.CopyToClipboard(text)))

      case Cut =>
         if state.selection.nonEmpty then
            val text = state |> getSelectedText(pieceTable)
            val newState = state
               |> deleteSelection(pieceTable)
               |> ensureCaretVisible(metrics, pieceTable.lineCount)

            (
               newState, 
               List(Effect.TextChanged, Effect.CopyToClipboard(text)),
            )
         else
            Effectful.pure(state)

      case RequestPaste => 
         (state, List(Effect.RequestPaste))

      case Paste(pastedText) =>
         val newState = state
            |> when(_.selection.nonEmpty, deleteSelection(pieceTable))
            |> insertText(pieceTable, metrics, pastedText)

         (
            newState, 
            List(Effect.TextChanged),
         )

      case MousePressed(x, y, shift) =>
         if scrollbarVisible(metrics, pieceTable.lineCount) && isOverScrollbar(metrics, x) then
            val newState = state |> beginScrollbarDrag(metrics, pieceTable.lineCount, y)

            (newState, List(Effect.RequestFocus))    
         else
            val clicked = state |> pointToPiecePos(pieceTable, metrics, x, y)

            val newState = 
               if shift then state 
                  |> beginSelectionIfNeeded
                  |> (s => s.mapCaret(_.moveTo(clicked.line, clicked.ch)))
                  |> ensureCaretVisible(metrics, pieceTable.lineCount)
               else state
                  |> resetSelection
                  |> (s => 
                        s.copy(
                           selection = s.selection.mapAnchor(_ => Position(clicked.line, clicked.ch)),
                           caret = s.caret.moveTo(clicked.line, clicked.ch),
                           draggingSelection = true,
                        )
                     )
                  |> ensureCaretVisible(metrics, pieceTable.lineCount)

            (
               newState,
               List(
                  Effect.RequestFocus,
                  Effect.UpdateEditorStatus(clicked.ch + 1, clicked.line + 1)
               )
            )

      case MouseReleased(x: Int, y: Int) =>
         Effectful.pure(state.copy(
            draggingScrollbar = false,
            draggingSelection = false,
         ))

      case MouseMoved(x: Int, y: Int) =>
         val nowHovering = scrollbarVisible(metrics, pieceTable.lineCount) && isOverScrollbar(metrics, x)
         val newState = state
            |> when(_.hoveringScrollbar != nowHovering, s => s.mapHoveringScrollbar(_ => nowHovering))

         Effectful.pure(newState)

      case MouseDragged(x: Int, y: Int) =>
         if state.draggingScrollbar then
            val lineHeight = metrics.fontMetrics.getHeight()
            val contentHeight = pieceTable.lineCount * lineHeight
            val trackHeight = metrics.size.height
            val maxScrollY = Math.max(0, contentHeight - trackHeight)

            if maxScrollY != 0 then
               val thumbHeight = EditorStyleMetrics.computeThumbHeight(trackHeight, contentHeight)
               val maxThumbY = Math.max(1, trackHeight - thumbHeight)
               val deltaY = y - state.scroll.dragStartY
               val deltaScroll = (deltaY.toLong * maxScrollY / maxThumbY).toInt
               val newState = state
                  |> setScrollY(metrics, pieceTable.lineCount, state.scroll.dragStartScrollY + deltaScroll)

               Effectful.pure(newState)
            else
               Effectful.pure(state)

         else if state.draggingSelection then
            val pos = state |> pointToPiecePos(pieceTable, metrics, x, y)
            val newState = state
               |> (s => s.copy(caret = s.caret.moveTo(pos.ch, pos.line)))
               |> ensureCaretVisible(metrics, pieceTable.lineCount)

            (newState, List(Effect.UpdateEditorStatus(pos.ch + 1, pos.line + 1)))

         else
            Effectful.pure(state)

      case Scroll(lines) =>
         val lineHeight = metrics.fontMetrics.getHeight()
         val newState = state |> setScrollY(
            metrics,
            pieceTable.lineCount,
            state.scroll.y + lines * lineHeight * EditorStyleMetrics.scrollLinesPerNotch,
         )
         
         Effectful.pure(newState)

      // TODO: undo, redo
      case Undo => Effectful.pure(state)

      case Redo => Effectful.pure(state)

   def beginScrollbarDrag(
      metrics: EditorStyleMetrics,
      lineCount: Int,
      clickY: Int,
   )(state: EditorState): EditorState =
      val lineHeight = metrics.fontMetrics.getHeight()
      val contentHeight = lineCount * lineHeight
      val trackHeight = metrics.size.height
      val thumbHeight = EditorStyleMetrics.computeThumbHeight(trackHeight, contentHeight)
      val maxThumbY = Math.max(1, trackHeight - thumbHeight)
      val maxScrollY = Math.max(0, contentHeight - trackHeight)
      val desiredThumbY = Math.max(0, Math.min(maxThumbY, clickY - thumbHeight / 2))
      val scrolled = state |> setScrollY(metrics, lineCount,
         if maxThumbY == 0 then 
            0 
         else 
            (desiredThumbY.toLong * maxScrollY / maxThumbY).toInt
      )

      scrolled.copy(
         draggingScrollbar = true,
         scroll = state.scroll.copy(
            dragStartY = clickY, 
            dragStartScrollY = state.scroll.y,
         ),
      )

   def setScrollY(
      metrics: EditorStyleMetrics,
      lineCount: Int,
      value: Int,
   )(state: EditorState): EditorState = 
      state.mapScroll(_.mapY(_ => clampScrollY(metrics, lineCount)(value)))

   def isOverScrollbar(metrics: EditorStyleMetrics, x: Int): Boolean = 
      x >= metrics.size.width - EditorStyleMetrics.scrollbarWidth

   def scrollbarVisible(metrics: EditorStyleMetrics, lineCount: Int): Boolean =
      lineCount * metrics.fontMetrics.getHeight() > metrics.size.height

   def pointToPiecePos(
      pieceTable: MutablePieceTable,
      metrics: EditorStyleMetrics,
      x: Int, 
      y: Int,
   )(state: EditorState): PiecePos = 
      val charWidth = metrics.fontMetrics.charWidth('W')
      val lineHeight = metrics.fontMetrics.getHeight()

      val adjustedX = x - state.gutterWidth
      val adjustedY = y + state.scroll.y

      val line = Math.max(0, Math.min(adjustedY / lineHeight, pieceTable.lineCount - 1))
      val rawCh = Math.max(0, (adjustedX + 5) / charWidth)
      val lineLength = 
         if line < pieceTable.lineCount then 
            pieceTable.lines(line).length 
         else 
            0
      val ch = Math.max(0, Math.min(rawCh, lineLength))

      PiecePos(line, ch)

   def insertText(
      pieceTable: MutablePieceTable, 
      metrics: EditorStyleMetrics,
      text: String,
   )(state: EditorState): EditorState = 
      text.foldLeft(state) { (state, c) =>
         if c == '\n' then 
            state |> insertNewline(pieceTable, metrics)
         else if c != '\r' then 
            state |> insertChar(pieceTable, c)
         else
            state
      }

   def selectAllLines(pieceTable: MutablePieceTable)(state: EditorState): EditorState = 
      val lastLine = pieceTable.lineCount - 1
      state.copy(
         selection = state.selection.mapAnchor(_ => Position.zero),
         caret = state.caret.moveTo(lastLine, pieceTable.lines(lastLine).length),
      ) 

   def getSelectedText(pieceTable: MutablePieceTable)(state: EditorState): String =
      val start = state.selection.start
      val end = state.selection.end

      if start.line == end.line then
         return pieceTable.lines(start.line).substring(start.col, end.col)

      var sb = new StringBuilder()
         .append(pieceTable.lines(start.line).substring(start.col))
         .append('\n')

      for (line <- start.line + 1 until end.line) {
         sb = sb.append(pieceTable.lines(line)).append('\n')
      }

      sb.append(pieceTable.lines(end.line).substring(0, end.col))
      sb.toString()


   def moveCaretHome(state: EditorState): EditorState = 
      state.mapCaret(_.copy(
         position = Position(state.caret.position.line, 0),
         desiredColumn = None,
      ))

   def moveCaretEnd(pieceTable: MutablePieceTable)(state: EditorState): EditorState = 
      state.mapCaret(_.copy(
         position = Position(state.caret.position.line, lineLength(pieceTable, state.caret.position.line)),
         desiredColumn = None,
      ))

   def moveCaretVertical(
      pieceTable: MutablePieceTable, 
      deltaLine: Int,
   )(state: EditorState): EditorState =
      val targetLine = Math.max(0, Math.min(pieceTable.lineCount - 1, state.caret.position.line + deltaLine))
      val column = 
         if state.caret.checkDesiredColumn(col => col >= 0) then 
            state.caret.desiredColumn.get
         else 
            state.caret.position.col

      val clampedColumn = Math.min(column, lineLength(pieceTable, targetLine))

      state.mapCaret(_.copy(
         position = Position(targetLine, clampedColumn),
         desiredColumn = Some(column),
      ))

   def moveCaretRight(pieceTable: MutablePieceTable)(state: EditorState): EditorState = 
      val x = state.caret.position.col
      val y = state.caret.position.line
      val moved = 
         if x < lineLength(pieceTable, y) then
            state.mapCaret(_.moveTo(y, x + 1))
         else if y < pieceTable.lineCount - 1 then
            state.mapCaret(_.moveTo(y + 1, 0))
         else 
            state

      moved.mapCaret(_.mapDesiredColumn(_ => None))

   def moveCaretLeft(pieceTable: MutablePieceTable)(state: EditorState): EditorState = 
      val x = state.caret.position.col
      val y = state.caret.position.line
      val moved = 
         if x > 0 then
            state.mapCaret(_.moveTo(y, x - 1))
         else if y > 0 then
            state.mapCaret(_.moveTo(y - 1, lineLength(pieceTable, y - 1)))
         else
            state

      moved.mapCaret(_.mapDesiredColumn(_ => None))

   def lineLength(pieceTable: MutablePieceTable, line: Int): Int =
      if line >= 0 && line < pieceTable.lineCount then 
         pieceTable.lines(line).length
      else 
         0

   def beginSelectionIfNeeded(state: EditorState): EditorState =
      state |> when(_.selection.isEmpty, resetSelection)

   def resetSelection(state: EditorState): EditorState =
      state.mapSelection(_ => Selection.reset(state.caret))

   def insertNewline(
      pieceTable: MutablePieceTable, 
      metrics: EditorStyleMetrics,
   )(state: EditorState): EditorState = 
      val x = state.caret.position.col
      val y = state.caret.position.line

      pieceTable.insert("\n", new PiecePos(y, x))

      state.copy(
         scroll = state.scroll.mapY(clampScrollY(metrics, pieceTable.lineCount)),
         caret = state.caret.moveTo(y + 1, 0),
         gutterWidth = updatedGutterWidth(metrics, pieceTable.lineCount),
      )

   def deleteForward(
      metrics: EditorStyleMetrics, 
      pieceTable: MutablePieceTable,
   )(state: EditorState): EditorState =
      val x = state.caret.position.col
      val y = state.caret.position.line
      val lineLength = pieceTable.lines(y).length
      val lastLineIndex = pieceTable.lineCount - 1

      if x < lineLength then
         pieceTable.delete(new PiecePos(y, x + 1))
         state
      else if y < lastLineIndex then
         pieceTable.delete(new PiecePos(y + 1, 0))
         state.copy(
            scroll = state.scroll.mapY(clampScrollY(metrics, pieceTable.lineCount)),
            gutterWidth = updatedGutterWidth(metrics, pieceTable.lineCount),
         )
      else
         state

   def deleteBackward(
      metrics: EditorStyleMetrics, 
      pieceTable: MutablePieceTable,
   )(state: EditorState): EditorState =
      val x = state.caret.position.col
      val y = state.caret.position.line
      val prevLineLengthBeforeMerge = 
         if y > 0 then 
            pieceTable.lines(y - 1).length 
         else 
            0

      pieceTable.delete(new PiecePos(y, x))

      if x > 0 then
         state.mapCaret(_.moveTo(y, x - 1))
      else if y > 0 then
         state.copy(
            gutterWidth = updatedGutterWidth(metrics, pieceTable.lineCount),
            scroll = state.scroll.mapY(clampScrollY(metrics, pieceTable.lineCount)),
            caret = state.caret.moveTo(y - 1, prevLineLengthBeforeMerge),
         )
      else
         state

   def deleteSelection(pieceTable: MutablePieceTable)(state: EditorState): EditorState =
      if state.selection.isEmpty then 
         state
      else
         val start = state.selection.start
         val end = state.selection.end

         pieceTable.deleteRange(
            new PiecePos(start.line, start.col),
            new PiecePos(end.line, end.col)
         )

         val newCaret = state.caret.moveTo(start.line, start.col)

         state.copy(
            caret = newCaret,
            selection = Selection.reset(newCaret),
         )

   def insertChar(
      pieceTable: MutablePieceTable,
      ch: Char,
   )(state: EditorState): EditorState =
      val x = state.caret.position.col
      val y = state.caret.position.line

      pieceTable.insert(String.valueOf(ch), new PiecePos(y, x))
      state.mapCaret(_.moveTo(y, x + 1))

   def ensureCaretVisible(
      metrics: EditorStyleMetrics,
      lineCount: Int,
   )(state: EditorState): EditorState =
      val lineHeight = metrics.fontMetrics.getHeight()
      val caretTop = state.caret.position.line * lineHeight
      val caretBottom = caretTop + lineHeight

      val scrolled = 
         if caretTop < state.scroll.y then
            state.mapScroll(_.mapY(_ => caretTop))
         else if caretBottom > state.scroll.y + metrics.size.height then
            state.mapScroll(_.mapY(_ => caretBottom - metrics.size.height))
         else
            state

      scrolled.mapScroll(_.mapY(clampScrollY(metrics, lineCount)))

   def clampScrollY(metrics: EditorStyleMetrics, lineCount: Int)(y: Int): Int =
      val lineHeight = metrics.fontMetrics.getHeight()
      val maxScrollY = Math.max(0, lineCount * lineHeight - metrics.size.height)
      
      Math.max(0, Math.min(y, maxScrollY))

   def updatedGutterWidth(metrics: EditorStyleMetrics, lineCount: Int): Int =
      val digits = Math.max(2, String.valueOf(lineCount).length)
      EditorStyleMetrics.gutterPadding + digits * metrics.fontMetrics.charWidth('0') + EditorStyleMetrics.gutterRightMargin
