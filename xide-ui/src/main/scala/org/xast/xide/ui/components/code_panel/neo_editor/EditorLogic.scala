package org.xast.xide.ui.components.code_panel.neo_editor

import org.xast.xide.ui.components.code_panel.neo_editor.EditorAction.TypeChar
import scala.swing.Rectangle
import org.xast.xide.ui.components.code_panel.neo_editor.Effect.RepaintCanvas

// FIXME: move to utils after scala rewrite
extension [A](value: A)
   infix def |>[B](f: A => B): B = f(value)

def when[A](pred: A => Boolean, f: A => A)(value: A): A =
   if pred(value) then f(value) else value

def unless[A](pred: A => Boolean, f: A => A)(value: A): A =
   if !pred(value) then f(value) else value

type Effectful[T] = (T, List[Effect])

object Effectful:

   def pure[T](value: T): Effectful[T] = (value, List.empty)

enum Effect:
   case CopyToClipboard(text: String)
   case RequestPaste
   case TextChanged
   case RepaintCanvas(amount: RepaintAmount)

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
      case TypeChar(c) =>
         val newState = state
            |> when(_.selection.nonEmpty, deleteSelection(pieceTable))
            |> insertChar(c, pieceTable)
            |> ensureCaretVisible(style, pieceTable.lineCount)

         (
            newState, 
            List(
               Effect.TextChanged, 
               RepaintCanvas(RepaintAmount.All),
            ),
         )

      case _ => Effectful.pure(state)

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
