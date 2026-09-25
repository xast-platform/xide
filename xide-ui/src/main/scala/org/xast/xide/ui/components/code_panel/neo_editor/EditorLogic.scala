package org.xast.xide.ui.components.code_panel.neo_editor

import scala.swing.Rectangle
import org.xast.xide.ui.components.code_panel.neo_editor.EditorAction.MoveLeft
import org.xast.xide.ui.components.code_panel.neo_editor.EditorAction.MoveRight

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

enum RepaintAmount:
   case All
   case Rect(rect: Rectangle)

object EditorLogic:

   def update(
      pieceTable: MutablePieceTable,
      state: EditorState,
      style: EditorStyle,
      action: EditorAction,
   ): Effectful[EditorState] = action match
      case EditorAction.TypeChar(c) =>
         val newState = state
            |> when(_.selection.nonEmpty, deleteSelection(pieceTable))
            |> insertChar(c, pieceTable)
            |> ensureCaretVisible(style, pieceTable.lineCount)

         (newState, List(Effect.TextChanged))

      case EditorAction.Backspace => 
         val newState = state
            |> whenElse(_.selection.nonEmpty, deleteSelection(pieceTable), deleteBackward(style, pieceTable))

         (newState, List(Effect.TextChanged))

      case EditorAction.Delete =>
         val newState = state
            |> whenElse(_.selection.nonEmpty, deleteSelection(pieceTable), deleteForward(style, pieceTable))

         (newState, List(Effect.TextChanged))

      case EditorAction.Enter =>
         val newState = state
            |> when(_.selection.nonEmpty, deleteSelection(pieceTable))
            |> insertNewline(pieceTable, style)

         (newState, List(Effect.TextChanged))
      
      case MoveLeft(shift) => 
         val newState = state
            |> whenElse(_ => shift, beginSelectionIfNeeded, resetSelection)
            |> moveCaretLeft(pieceTable)

         Effectful.pure(newState)

      case MoveRight(shift) => 
         val newState = state
            |> whenElse(_ => shift, beginSelectionIfNeeded, resetSelection)
            |> moveCaretRight(pieceTable)

         Effectful.pure(newState)

      case _ => Effectful.pure(state)

   def moveCaretRight(pieceTable: MutablePieceTable)(state: EditorState): EditorState = 
      val x = state.caret.position.col
      val y = state.caret.position.line
      val moved = 
         if x < lineLength(y) then
            state.mapCaret(_.moveTo(x + 1, y))
         else if y < pieceTable.lineCount - 1 then
            state.mapCaret(_.moveTo(0, y + 1))
         else 
            state

      moved.mapCaret(_.mapDesiredColumn(_ => None))

   def moveCaretLeft(pieceTable: MutablePieceTable)(state: EditorState): EditorState = 
      val x = state.caret.position.col
      val y = state.caret.position.line
      val moved = 
         if x > 0 then
            state.mapCaret(_.moveTo(x - 1, y))
         else if y > 0 then
            state.mapCaret(_.moveTo(lineLength(pieceTable, y - 1), y - 1))
         else
            state

      moved.mapCaret(_.mapDesiredColumn(_ => None))

   def lineLength(pieceTable: MutablePieceTable, line: Int): Int =
      if line >= 0 && line < pieceTable.lines.size then 
         pieceTable.lines(line).length() 
      else 
         0

   def beginSelectionIfNeeded(state: EditorState): EditorState =
      state |> when(_.selection.isEmpty, resetSelection)

   def resetSelection(state: EditorState): EditorState =
      state.mapSelection(_ => Selection.reset(state.caret))

   def insertNewline(
      pieceTable: MutablePieceTable, 
      style: EditorStyle,
   )(state: EditorState): EditorState = 
      val x = state.caret.position.col
      val y = state.caret.position.line

      pieceTable.insert("\n", new PiecePos(y, x))

      state.copy(
         scroll = state.scroll.mapY(clampScrollY(style, pieceTable.lineCount)),
         caret = state.caret.moveTo(0, y + 1),
         gutterWidth = updatedGutterWidth(style, pieceTable.lineCount),
      )

   def deleteForward(
      style: EditorStyle, 
      pieceTable: MutablePieceTable,
   )(state: EditorState): EditorState =
      val x = state.caret.position.col
      val y = state.caret.position.line
      val lineLength = pieceTable.lines(y).length()
      val lastLineIndex = pieceTable.lines.size - 1

      if x < lineLength then
         pieceTable.delete(new PiecePos(y, x + 1))
         state
      else if y < lastLineIndex then
         pieceTable.delete(new PiecePos(y + 1, 0))
         state.copy(
            scroll = state.scroll.mapY(clampScrollY(style, pieceTable.lineCount)),
            gutterWidth = updatedGutterWidth(style, pieceTable.lineCount),
         )
      else
         state

   def deleteBackward(
      style: EditorStyle, 
      pieceTable: MutablePieceTable,
   )(state: EditorState): EditorState =
      val x = state.caret.position.col
      val y = state.caret.position.line
      val prevLineLengthBeforeMerge = 
         if y > 0 then 
            pieceTable.lines(y - 1).length() 
         else 
            0

      pieceTable.delete(new PiecePos(y, x))

      if x > 0 then
         state.mapCaret(_.moveTo(x - 1, y))
      else if y > 0 then
         state.copy(
            gutterWidth = updatedGutterWidth(style, pieceTable.lineCount),
            scroll = state.scroll.mapY(clampScrollY(style, pieceTable.lineCount)),
            caret = state.caret.moveTo(prevLineLengthBeforeMerge, y - 1),
         )
      else
         state

   def deleteSelection(pieceTable: MutablePieceTable)(state: EditorState): EditorState =
      if state.selection.isEmpty then 
         state
      else
         val start = state.selection.anchor
         val end = state.selection.active

         pieceTable.deleteRange(
            new PiecePos(start.line, start.col), 
            new PiecePos(end.line, end.col)
         )

         state.copy(
            caret = state.caret.moveTo(start.col, start.line),
            selection = Selection.reset(state.caret),
         )

   def insertChar(
      ch: Char,
      pieceTable: MutablePieceTable,
   )(state: EditorState): EditorState =
      val x = state.caret.position.col
      val y = state.caret.position.line

      pieceTable.insert(String.valueOf(ch), new PiecePos(y, x))
      state.mapCaret(_.moveTo(x + 1, y))

   def ensureCaretVisible(
      style: EditorStyle,
      lineCount: Int,
   )(state: EditorState): EditorState =
      val lineHeight = style.fontMetrics.getHeight()
      val caretTop = state.caret.position.line * lineHeight
      val caretBottom = caretTop + lineHeight
      val previousScrollY = state.scroll.y

      val scrolled = 
         if caretTop < state.scroll.y then
            state.mapScroll(_.mapY(_ => caretTop))
         else if caretBottom > state.scroll.y + style.size.height then
            state.mapScroll(_.mapY(_ => caretBottom - style.size.height))
         else
            state

      scrolled.mapScroll(_.mapY(clampScrollY(style, lineCount)))

   def clampScrollY(
      style: EditorStyle, 
      lineCount: Int,
   ): Int => Int =
      val lineHeight = style.fontMetrics.getHeight()
      val maxScrollY = Math.max(0, lineCount * lineHeight - style.size.height)
      
      y => Math.max(0, Math.min(y, maxScrollY))

   def updatedGutterWidth(style: EditorStyle, lineCount: Int): Int =
      val digits = Math.max(2, String.valueOf(lineCount).length())
      EditorStyle.gutterPadding + digits * style.fontMetrics.charWidth('0') + EditorStyle.gutterRightMargin
