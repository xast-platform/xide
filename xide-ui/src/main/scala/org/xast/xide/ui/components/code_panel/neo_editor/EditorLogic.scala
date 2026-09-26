package org.xast.xide.ui.components.code_panel.neo_editor

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
   case RestartCaretBlink
   case RepaintAll

object EditorLogic:

   def update(
      pieceTable: MutablePieceTable,
      model: EditorModel,
      metrics: EditorStyleMetrics,
      action: EditorAction,
      now: Long,
   ): Effectful[EditorModel] =
      val (next, effects) = action match
         case Undo => undo(pieceTable, model, metrics)
         case Redo => redo(pieceTable, model, metrics)
         case _ => updateAndRecord(pieceTable, model, metrics, action, now)

      if next.state.caret.position != model.state.caret.position then
         val pos = next.state.caret.position
         (
            next.copy(state = next.state.mapCaretVisible(_ => true)),
            effects ++ List(Effect.RestartCaretBlink, Effect.UpdateEditorStatus(pos.col + 1, pos.line + 1)),
         )
      else
         (next, effects)

   private def updateAndRecord(
      pieceTable: MutablePieceTable,
      model: EditorModel,
      metrics: EditorStyleMetrics,
      action: EditorAction,
      now: Long,
   ): Effectful[EditorModel] =
      val prev = model.state
      val (next, effects) = updateAction(pieceTable, prev, metrics, action)

      val edits = pieceTable.drainEdits()

      val history = 
         if edits.isEmpty then 
            model.history
         else 
            EditorHistory(
               undo = pushHistory(pieceTable, model.history.undo, HistoryEntry(edits, prev, next, now)),
               redo = Nil,
            )

      (EditorModel(next, history), effects)

   def updateAction(
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
            |> moveCaret(shift, moveCaretLeft(pieceTable))
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case MoveRight(shift) => 
         val newState = state
            |> moveCaret(shift, moveCaretRight(pieceTable))
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case MoveDown(shift) =>
         val newState = state
            |> moveCaret(shift, moveCaretVertical(pieceTable, 1))
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case MoveUp(shift) =>
         val newState = state
            |> moveCaret(shift, moveCaretVertical(pieceTable, -1))
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case PressHome(shift) =>
         val newState = state
            |> moveCaret(shift, moveCaretHome)
            |> ensureCaretVisible(metrics, pieceTable.lineCount)

         Effectful.pure(newState)

      case PressEnd(shift) =>
         val newState = state
            |> moveCaret(shift, moveCaretEnd(pieceTable))
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
               state
                  |> moveCaret(shift, _.mapCaret(_.moveTo(clicked.line, clicked.ch).mapDesiredColumn(_ => None)))
                  |> (_.copy(draggingSelection = true))
                  |> ensureCaretVisible(metrics, pieceTable.lineCount)

            (
               newState,
               List(Effect.RequestFocus)
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
               |> (_.mapCaret(_.moveTo(pos.line, pos.ch)))
               |> selectToCaret
               |> ensureCaretVisible(metrics, pieceTable.lineCount)

            Effectful.pure(newState)

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

      case Undo | Redo => Effectful.pure(state)

      case BlinkCaret => Effectful.pure(state.mapCaretVisible(!_))

   def undo(
      pieceTable: MutablePieceTable,
      model: EditorModel,
      metrics: EditorStyleMetrics,
   ): Effectful[EditorModel] = model.history.undo match
      case entry :: rest =>
         pieceTable.withoutRecording(entry.edits.reverseIterator.foreach(applyInverse(pieceTable)))

         val state = model.state |> restoreFrom(entry.before, pieceTable, metrics)
         val history = EditorHistory(rest, entry :: model.history.redo)

         (EditorModel(state, history), List(Effect.TextChanged, Effect.RepaintAll))

      case Nil => Effectful.pure(model)

   def redo(
      pieceTable: MutablePieceTable,
      model: EditorModel,
      metrics: EditorStyleMetrics,
   ): Effectful[EditorModel] = model.history.redo match
      case entry :: rest =>
         pieceTable.withoutRecording(entry.edits.foreach(applyForward(pieceTable)))

         val state = model.state |> restoreFrom(entry.after, pieceTable, metrics)
         val history = EditorHistory(entry :: model.history.undo, rest)

         (EditorModel(state, history), List(Effect.TextChanged, Effect.RepaintAll))

      case Nil => Effectful.pure(model)

   def restoreFrom(
      snapshot: EditorState,
      pieceTable: MutablePieceTable,
      metrics: EditorStyleMetrics,
   )(state: EditorState): EditorState =
      state.copy(
         caret = snapshot.caret,
         selection = snapshot.selection,
         scroll = state.scroll.mapY(clampScrollY(metrics, pieceTable.lineCount)),
         gutterWidth = updatedGutterWidth(metrics, pieceTable.lineCount),
      ) |> ensureCaretVisible(metrics, pieceTable.lineCount)

   def applyForward(pieceTable: MutablePieceTable)(edit: Edit): Unit = edit match
      case Edit.Insert(at, addStart, length) =>
         pieceTable.insert(pieceTable.addedText(addStart, length), toPiecePos(at))

      case Edit.Delete(from, text) =>
         pieceTable.deleteRange(toPiecePos(from), toPiecePos(endOf(from, text)))

   def applyInverse(pieceTable: MutablePieceTable)(edit: Edit): Unit = edit match
      case Edit.Insert(at, addStart, length) =>
         val text = pieceTable.addedText(addStart, length)
         pieceTable.deleteRange(toPiecePos(at), toPiecePos(endOf(at, text)))

      case Edit.Delete(from, text) =>
         pieceTable.insert(text, toPiecePos(from))

   def pushHistory(
      pieceTable: MutablePieceTable,
      stack: List[HistoryEntry],
      entry: HistoryEntry,
   ): List[HistoryEntry] = stack match
      case last :: rest if entry.time - last.time < EditorHistory.groupTimeoutMs && entry.edits.size == 1 =>
         mergeEdits(pieceTable, last.edits.last, entry.edits.head) match
            case Some(merged) =>
               last.copy(edits = last.edits.init :+ merged, after = entry.after, time = entry.time) :: rest
            case None =>
               (entry :: stack).take(EditorHistory.maxDepth)

      case _ => 
         (entry :: stack).take(EditorHistory.maxDepth)

   def mergeEdits(pieceTable: MutablePieceTable, prev: Edit, next: Edit): Option[Edit] = 
      (prev, next) match
         case (Edit.Insert(at1, start1, len1), Edit.Insert(at2, start2, len2)) =>
            val prevText = pieceTable.addedText(start1, len1)
            val nextText = pieceTable.addedText(start2, len2)
            val contiguous = start2 == start1 + len1 && at2 == endOf(at1, prevText)
            val wordBoundary = prevText.last.isWhitespace && !nextText.head.isWhitespace

            if contiguous && !nextText.contains('\n') && !prevText.contains('\n') && !wordBoundary then
               Some(Edit.Insert(at1, start1, len1 + len2))
            else 
               None

         case (Edit.Delete(from1, text1), Edit.Delete(from2, text2)) 
            if !text1.contains('\n') && !text2.contains('\n') =>
            if endOf(from2, text2) == from1 then
               Some(Edit.Delete(from2, text2 + text1))
            else if from2 == from1 then
               Some(Edit.Delete(from1, text1 + text2))
            else 
               None

         case _ => None

   def endOf(start: Position, text: String): Position =
      val newlines = text.count(_ == '\n')

      if newlines == 0 then 
         Position(start.line, start.col + text.length)
      else 
         Position(start.line + newlines, text.length - text.lastIndexOf('\n') - 1)

   def toPiecePos(position: Position): PiecePos =
      PiecePos(position.line, position.col)

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
         selection = Selection(Position.zero, Position(lastLine, lineLength(pieceTable, lastLine))),
         caret = state.caret.moveTo(lastLine, lineLength(pieceTable, lastLine)),
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

   def moveCaret(shift: Boolean, move: EditorState => EditorState)(state: EditorState): EditorState =
      if shift then state
         |> beginSelectionIfNeeded
         |> move
         |> selectToCaret
      else state
         |> move
         |> resetSelection

   def selectToCaret(state: EditorState): EditorState =
      state.mapSelection(_.mapActive(_ => state.caret.position))

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
