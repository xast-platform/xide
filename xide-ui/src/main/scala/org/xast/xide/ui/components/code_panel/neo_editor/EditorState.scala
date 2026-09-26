package org.xast.xide.ui.components.code_panel.neo_editor

import scala.math.Ordering.Implicits.infixOrderingOps

case class EditorModel(
   state: EditorState,
   history: EditorHistory,
)

case class EditorHistory(
   undo: List[HistoryEntry] = Nil,
   redo: List[HistoryEntry] = Nil,
)

object EditorHistory:
   final val maxDepth: Int = 1000
   final val groupTimeoutMs: Long = 1000

case class HistoryEntry(
   edits: Vector[Edit],
   before: EditorState,
   after: EditorState,
   time: Long,
)

case class EditorState(
   caret: Caret,
   selection: Selection,
   scroll: Scroll,
   draggingSelection: Boolean = false,
   draggingScrollbar: Boolean = false,
   hoveringScrollbar: Boolean = false,
   caretVisible: Boolean = true,
   gutterWidth: Int = EditorStyleMetrics.defaultGutterWidth,
):

   def mapCaret(f: Caret => Caret): EditorState = 
      copy(caret = f(caret))

   def mapSelection(f: Selection => Selection): EditorState = 
      copy(selection = f(selection))

   def mapScroll(f: Scroll => Scroll): EditorState = 
      copy(scroll = f(scroll))

   def mapDraggingScrollbar(f: Boolean => Boolean): EditorState = 
      copy(draggingScrollbar = f(draggingScrollbar))

   def mapHoveringScrollbar(f: Boolean => Boolean): EditorState = 
      copy(hoveringScrollbar = f(hoveringScrollbar))

   def mapCaretVisible(f: Boolean => Boolean): EditorState = 
      copy(caretVisible = f(caretVisible))

   def mapGutterWidth(f: Int => Int): EditorState = 
      copy(gutterWidth = f(gutterWidth))

object EditorState:

   def initial: EditorState = EditorState(
      caret      = Caret.zero,
      selection  = Selection.zero,
      scroll     = Scroll.zero,
   )

/**
  * 
  *
  * @param line
  * @param col
  */
case class Position(line: Int, col: Int) extends Ordered[Position]:

   override def compare(that: Position): Int =
      val c = line.compare(that.line)
      if c != 0 then c else col.compare(that.col)

object Position:
   def zero: Position = Position(0, 0)

/**
  * 
  *
  * @param anchor
  * @param active
  */
case class Selection(
   anchor: Position, 
   active: Position,
):

   def start: Position = anchor.min(active)

   def end: Position = anchor.max(active)

   def isEmpty: Boolean = anchor == active

   def nonEmpty: Boolean = !isEmpty

   def mapAnchor(f: Position => Position): Selection = 
      copy(anchor = f(anchor))

   def mapActive(f: Position => Position): Selection = 
      copy(active = f(active))

object Selection:

   def zero: Selection = 
      Selection(Position.zero, Position.zero)

   def reset(caret: Caret): Selection =
      Selection(caret.position, caret.position)

/**
  * 
  *
  * @param position
  * @param desiredColumn
  */
case class Caret(
   position: Position,
   desiredColumn: Option[Int] = None,
):

   def checkDesiredColumn(pred: Int => Boolean): Boolean =
      desiredColumn.filter(pred).isDefined

   def mapDesiredColumn(f: Option[Int] => Option[Int]): Caret = 
      copy(desiredColumn = f(desiredColumn))

   def moveTo(line: Int, col: Int): Caret =
      copy(position = Position(line, col))

object Caret:

   def zero: Caret =
      Caret(Position.zero)

case class Scroll(
   y: Int,
   dragging: Boolean = false,
   dragStartY: Int = 0,
   dragStartScrollY: Int = 0
):

   def mapY(f: Int => Int): Scroll = 
      copy(y = f(y))

   def mapDragging(f: Boolean => Boolean): Scroll = 
      copy(dragging = f(dragging))

   def mapDragStartY(f: Int => Int): Scroll = 
      copy(dragStartY = f(dragStartY))

   def mapDragStartScrollY(f: Int => Int): Scroll = 
      copy(dragStartScrollY = f(dragStartScrollY))

object Scroll:

   def zero: Scroll =
      Scroll(0)