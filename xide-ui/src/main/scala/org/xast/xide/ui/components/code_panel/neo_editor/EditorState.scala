package org.xast.xide.ui.components.code_panel.neo_editor

import scala.math.Ordering.Implicits.infixOrderingOps

case class EditorState(
   caret: Caret,
   selection: Option[Selection],
   scroll: Scroll,
   draggingSelection: Boolean,
   hoveringScrollbar: Boolean
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

   def moveTo(line: Int, col: Int): Caret =
      this.copy(position = Position(line, col))

object Caret:

   def zero: Caret =
      Caret(Position.zero)

case class Scroll(
   y: Int,
   dragging: Boolean = false,
   dragStartY: Int = 0,
   dragStartScrollY: Int = 0
)