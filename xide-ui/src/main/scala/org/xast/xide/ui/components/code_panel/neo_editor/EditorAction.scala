package org.xast.xide.ui.components.code_panel.neo_editor

enum EditorAction:
   case TypeChar(ch: Char)
   case Backspace
   case Delete
   case Enter

   case MoveLeft
   case MoveRight
   case MoveUp
   case MoveDown
   case PressHome
   case PressEnd

   case SelectAll
   case Copy
   case Cut
   case Paste

   case MousePressed(x: Int, y: Int, shift: Boolean)
   case MouseDragged(x: Int, y: Int)
   case MouseReleased(x: Int, y: Int)

   case Scroll(lines: Int)