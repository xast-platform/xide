package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.RenderingHints
import scala.swing.Graphics2D
import java.awt.Color
import java.awt.Rectangle

object EditorRenderer:

   def paint(
      g: Graphics2D,
      pieceTable: MutablePieceTable,
      state: EditorState,
      metrics: EditorStyleMetrics,
   ): Unit =
      val width = metrics.size.width
      val height = metrics.size.height
      val lineHeight = metrics.fontMetrics.getHeight()
      val clip = Option(g.getClipBounds()).getOrElse(new Rectangle(0, 0, width, height))

      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
      g.setColor(metrics.bgColor)
      g.fillRect(clip.x, clip.y, clip.width, clip.height)

      val firstVisibleLine = Math.max(0, (state.scroll.y + clip.y) / lineHeight)
      val lastVisibleLine = Math.min(
         pieceTable.lineCount, 
         (state.scroll.y + clip.y + clip.height) / lineHeight + 1,
      )
      val contentX = state.gutterWidth
      val contentWidth = Math.max(0, width - state.gutterWidth - EditorStyleMetrics.scrollbarWidth)

      val contentG = g.create(contentX, 0, contentWidth, height).asInstanceOf[Graphics2D]
      contentG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
      contentG.setFont(metrics.font)
      contentG.translate(0, -state.scroll.y)

      paintSelection(contentG, pieceTable, state, metrics, firstVisibleLine, lastVisibleLine, lineHeight)
      paintText(contentG, metrics, pieceTable, lineHeight, firstVisibleLine, lastVisibleLine)
      paintCaret(contentG, state, metrics)
      contentG.dispose()

      paintGutter(g, state, metrics, firstVisibleLine, lastVisibleLine, lineHeight)
      paintScrollbar(g, pieceTable, state, metrics, width, height, lineHeight)

   def paintText(
      contentG: Graphics2D, 
      metrics: EditorStyleMetrics,
      pieceTable: MutablePieceTable,
      lineHeight: Int,
      firstVisibleLine: Int, 
      lastVisibleLine: Int, 
   ): Unit =
      contentG.setColor(metrics.fontColor)
      val lastLine = Math.min(lastVisibleLine, pieceTable.lineCount)
      for lineIndex <- firstVisibleLine until lastLine do
         val baselineY = lineHeight * lineIndex + metrics.fontMetrics.getAscent()
         contentG.drawString(pieceTable.lines(lineIndex), 0, baselineY)

   def paintSelection(
      g: Graphics2D,
      pieceTable: MutablePieceTable,
      state: EditorState,
      metrics: EditorStyleMetrics,
      firstVisibleLine: Int,
      lastVisibleLine: Int,
      lineHeight: Int,
   ): Unit =
      if state.selection.isEmpty then 
         return

      val start = state.selection.start
      val end = state.selection.end

      g.setColor(new Color(80, 140, 255, 70))
      val from = Math.max(start.line, firstVisibleLine)
      val to = Math.min(end.line, lastVisibleLine - 1)
      val charWidth = metrics.fontMetrics.charWidth('W')

      for line <- from to to do
         val text = if line < pieceTable.lineCount then pieceTable.lines(line) else ""
         val colStart = if line == start.line then start.col else 0
         val colEnd = if line == end.line then end.col else text.length()

         val x1 = Math.min(colStart, text.length()) * charWidth
         val x2 = Math.min(colEnd, text.length()) * charWidth
         val width = Math.max(x2 - x1, if colEnd > colStart then 0 else 6)

         g.fillRect(x1, lineHeight * line, Math.max(width, 6), lineHeight)

   def paintCaret(g: Graphics2D, state: EditorState, metrics: EditorStyleMetrics): Unit =
      if !state.caretVisible then
         return

      val deltaX = metrics.fontMetrics.charWidth('W')
      val deltaY = metrics.fontMetrics.getHeight()
      val caretHeight = metrics.fontMetrics.getHeight()
      val position = state.caret.position

      g.setColor(metrics.fontColor)
      g.fillRect(position.col * deltaX, position.line * deltaY, EditorStyleMetrics.caretWidth, caretHeight)

   def paintGutter(
      g: Graphics2D,
      state: EditorState,
      metrics: EditorStyleMetrics,
      firstVisibleLine: Int,
      lastVisibleLine: Int,
      lineHeight: Int,
   ): Unit =
      val base = metrics.bgColor
      val lineNumberColor = new Color(metrics.fontColor.getRed(), metrics.fontColor.getGreen(), metrics.fontColor.getBlue(), 140)
      val height = metrics.size.height

      g.setColor(base)
      g.fillRect(0, 0, state.gutterWidth, height)
      g.setFont(metrics.font)
      g.setColor(lineNumberColor)

      for lineIndex <- firstVisibleLine until lastVisibleLine do
         val label = String.valueOf(lineIndex + 1)
         val textWidth = metrics.fontMetrics.stringWidth(label)
         val y = lineHeight * lineIndex - state.scroll.y + metrics.fontMetrics.getAscent()
         g.drawString(label, state.gutterWidth - textWidth - EditorStyleMetrics.gutterRightMargin, y)

      g.setColor(metrics.xideStyle.shiftAccent(0.15f))
      g.drawLine(state.gutterWidth - 1, 0, state.gutterWidth - 1, height)

   def paintScrollbar(
      g: Graphics2D,
      pieceTable: MutablePieceTable,
      state: EditorState,
      metrics: EditorStyleMetrics,
      width: Int,
      height: Int,
      lineHeight: Int,
   ): Unit =
      val contentHeight = Math.max(1, pieceTable.lineCount * lineHeight)
      if contentHeight <= height then
         return

      val trackX = width - EditorStyleMetrics.scrollbarWidth
      g.setColor(
         if metrics.xideStyle.isDarkTheme then 
            new Color(255, 255, 255, 30) 
         else 
            new Color(0, 0, 0, 40)
      )
      g.fillRect(trackX, 0, EditorStyleMetrics.scrollbarWidth, height)

      val thumbHeight = EditorStyleMetrics.computeThumbHeight(height, contentHeight)
      val maxScrollY = contentHeight - height
      val maxThumbY = height - thumbHeight
      val thumbY = 
         if maxScrollY <= 0 then 
            0 
         else 
            (state.scroll.y.toLong * maxThumbY / maxScrollY).toInt

      g.setColor(
         if state.hoveringScrollbar || state.draggingScrollbar then
            metrics.xideStyle.shiftAccent(0.5f)
         else
            metrics.xideStyle.shiftAccent(0.35f)
      )
      g.fillRect(trackX + 2, thumbY, EditorStyleMetrics.scrollbarWidth - 4, thumbHeight)