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
         case Key.V if ctrl => Some(A.RequestPaste)
         case Key.Z if ctrl => Some(A.Undo)
         case Key.Y if ctrl => Some(A.Redo)

         case _             => None

   private def hasModifier(mods: Int, m: Int) = 
      (mods & m) != 0

   private val commandMask = 
      Key.Modifier.Control | Key.Modifier.Alt | Key.Modifier.Meta