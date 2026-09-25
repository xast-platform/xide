package org.xast.xide.ui.components.code_panel.neo_editor

enum EditorAction:
   case TypeChar(ch: Char)
   case Backspace
   case Delete
   case Enter

   case MoveLeft(shift: Boolean)
   case MoveRight(shift: Boolean)
   case MoveUp(shift: Boolean)
   case MoveDown(shift: Boolean)
   case PressHome(shift: Boolean)
   case PressEnd(shift: Boolean)

   case SelectAll
   case Copy
   case Cut
   case RequestPaste
   case Paste(pastedText: String)
   case Undo
   case Redo

   case MousePressed(x: Int, y: Int, shift: Boolean)
   case MouseDragged(x: Int, y: Int)
   case MouseReleased(x: Int, y: Int)

   case Scroll(lines: Int)