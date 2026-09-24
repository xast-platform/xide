package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.Color
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

object EditorSwingAdapter:
   val FRAME_FPS = 60
   val FRAME_INTERVAL_MS: Int = 1000 / FRAME_FPS

class EditorSwingAdapter(val editor: NeoEditor)

   

   // private val mouseHandler: MouseAdapter = new MouseAdapter():

      // override def mousePressed(e: MouseEvent): Unit = {
      //    editor.requestFocus()
      //    val x = e.getX()
      //    val y = e.getY()

      //    if editor.scrollbarVisible && editor.isOverScrollbar(x) then
      //       beginScrollbarDrag(y)
      //       return

      //    val prevSelection = selectionLinesOrEmpty()
      //    val clicked = pointToPiecePos(x, y)

      //    if (e.isShiftDown()) {
      //       beginSelectionIfNeeded()
      //       caret.moveTo(clicked.col, clicked.line)
      //       hasSelection = clicked != anchorPos()
      //    } else {
      //       clearSelection()
      //       selAnchorX = clicked.col
      //       selAnchorY = clicked.line
      //       caret.moveTo(clicked.col, clicked.line)
      //       draggingSelection = true
      //    }

      //    editorStatus.setCurrentChar(clicked.col + 1)
      //    editorStatus.setCurrentLine(clicked.line + 1)
      //    ensureCaretVisible()
      //    invalidateSelectionChange(prevSelection)
      // }

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
      //    setScrollY(scrollY + e.getWheelRotation() * lineHeight * SCROLL_LINES_PER_NOTCH)
      // }

// class EditorMouseAdapter(val editor: NeoEditor) extends MouseAdapter:
