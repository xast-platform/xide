package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.event.KeyEvent
import org.xast.xide.ui.components.code_panel.neo_editor.*
import scala.swing.event.*
import EditorAction as A

object EditorSwingAdapter:

   val toAction: PartialFunction[Event, A] =
      case KeyTyped(_, ch, mods, _)
         if ch != KeyEvent.CHAR_UNDEFINED &&
            !Character.isISOControl(ch) &&
            !hasModifier(mods, commandMask) => A.TypeChar(ch)

      case KeyPressed(_, key, mods, _)
         if keyAction(key, mods).isDefined => keyAction(key, mods).get

      case e: MousePressed => A.MousePressed(
         e.point.x, 
         e.point.y, 
         hasModifier(e.modifiers, Key.Modifier.Shift),
      )

      case e: MouseDragged => A.MouseDragged(e.point.x, e.point.y)
      
      case e: MouseReleased => A.MouseReleased(e.point.x, e.point.y)

      case e: MouseWheelMoved => A.Scroll(e.rotation)

   private def keyAction(key: Key.Value, mods: Int): Option[A] =
      val ctrl  = hasModifier(mods, Key.Modifier.Control)
      val shift = hasModifier(mods, Key.Modifier.Shift)

      key match
         case Key.BackSpace => Some(A.Backspace)
         case Key.Delete    => Some(A.Delete)
         case Key.Enter     => Some(A.Enter)

         case Key.Left      => Some(A.MoveLeft(shift))
         case Key.Right     => Some(A.MoveRight(shift))
         case Key.Up        => Some(A.MoveUp(shift))
         case Key.Down      => Some(A.MoveDown(shift))
         case Key.Home      => Some(A.PressHome(shift))
         case Key.End       => Some(A.PressEnd(shift))

         case Key.A if ctrl => Some(A.SelectAll)
         case Key.C if ctrl => Some(A.Copy)
         case Key.X if ctrl => Some(A.Cut)
         case Key.V if ctrl => Some(A.Paste)

         case _             => None

   private def hasModifier(mods: Int, m: Int) = 
      (mods & m) != 0

   private val commandMask = 
      Key.Modifier.Control | Key.Modifier.Alt | Key.Modifier.Meta

   

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
