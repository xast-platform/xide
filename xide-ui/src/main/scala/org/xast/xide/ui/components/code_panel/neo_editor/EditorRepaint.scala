package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.Rectangle

enum RepaintAmount:
   case All
   case Rects(rects: List[Rectangle])

object EditorRepaint:

   def diff(
      prev: EditorState,
      next: EditorState,
      metrics: EditorStyleMetrics,
      prevLineCount: Int,
      lineCount: Int,
      textChanged: Boolean,
   ): Option[RepaintAmount] =
      if prev.scroll.y != next.scroll.y || prev.gutterWidth != next.gutterWidth then
         return Some(RepaintAmount.All)

      if prev == next && !textChanged then
         return None

      val lineCountChanged = prevLineCount != lineCount

      val textRects = 
         if textChanged then 
            List(editedLinesRect(prev, next, metrics, lineCountChanged))
         else 
            Nil

      val selectionRects = 
         selectionDirtyLines(prev.selection, next.selection)
            .map((from, to) => linesRect(next, metrics, from, to))
            .toList

      val caretRects =
         if prev.caret.position != next.caret.position || prev.caretVisible != next.caretVisible then
            List(caretRect(prev, metrics), caretRect(next, metrics))
         else 
            Nil

      val scrollbarRects =
         if lineCountChanged
            || prev.hoveringScrollbar != next.hoveringScrollbar
            || prev.draggingScrollbar != next.draggingScrollbar
         then 
            List(scrollbarRect(metrics))
         else 
            Nil

      val rects = textRects ++ selectionRects ++ caretRects ++ scrollbarRects
      if rects.isEmpty then 
         None 
      else 
         Some(RepaintAmount.Rects(rects))

   private def editedLinesRect(
      prev: EditorState,
      next: EditorState,
      metrics: EditorStyleMetrics,
      lineCountChanged: Boolean,
   ): Rectangle =
      val from = List(prev.caret.position.line, prev.selection.start.line, next.caret.position.line).min
      val to = List(prev.caret.position.line, prev.selection.end.line, next.caret.position.line).max

      if lineCountChanged then
         val top = from * metrics.fontMetrics.getHeight() - next.scroll.y
         new Rectangle(0, top, metrics.size.width, Math.max(0, metrics.size.height - top))
      else 
         linesRect(next, metrics, from, to)

   private def selectionDirtyLines(prev: Selection, next: Selection): Option[(Int, Int)] =
      if prev.isEmpty && next.isEmpty then 
         None
      else if prev == next then 
         None
      else if prev.nonEmpty && next.nonEmpty && prev.anchor == next.anchor then
         Some((Math.min(prev.active.line, next.active.line), Math.max(prev.active.line, next.active.line)))
      else
         val ranges = List(prev, next).filter(_.nonEmpty)
         Some((ranges.map(_.start.line).min, ranges.map(_.end.line).max))

   private def linesRect(state: EditorState, metrics: EditorStyleMetrics, from: Int, to: Int): Rectangle =
      val lineHeight = metrics.fontMetrics.getHeight()
      new Rectangle(0, from * lineHeight - state.scroll.y, metrics.size.width, (to - from + 1) * lineHeight)

   private def caretRect(state: EditorState, metrics: EditorStyleMetrics): Rectangle =
      val charWidth = metrics.fontMetrics.charWidth('W')
      val lineHeight = metrics.fontMetrics.getHeight()
      val position = state.caret.position

      new Rectangle(
         state.gutterWidth + position.col * charWidth,
         position.line * lineHeight - state.scroll.y,
         EditorStyleMetrics.caretWidth,
         lineHeight,
      )

   private def scrollbarRect(metrics: EditorStyleMetrics): Rectangle =
      new Rectangle(
         metrics.size.width - EditorStyleMetrics.scrollbarWidth, 
         0, 
         EditorStyleMetrics.scrollbarWidth, 
         metrics.size.height,
      )
