package org.xast.xide.ui.components.code_panel.neo_editor

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

class MutablePieceTable(val content: String):

   private val originalBuffer: String   = content
   private var addBuffer: String        = ""
   private var pieceHead: Option[Piece] = Some(new Piece(0, content.length, Source.Original, None))
   private val lineCache: ArrayBuffer[String] = computeLines()
   private val pending: ArrayBuffer[Edit] = ArrayBuffer[Edit]()
   private var recording = true
   
   /**
     * Current line-by-line view of the document. Kept in sync incrementally
     * by insert/delete 
     */
   def lines: ArrayBuffer[String] = lineCache

   def drainEdits(): Vector[Edit] =
      val edits = pending.toVector; 
      pending.clear(); 
      edits

   def withoutRecording[A](f: => A): A =
      recording = false

      try f 
      finally recording = true

   def addedText(start: Int, length: Int): String =
      addBuffer.substring(start, start + length)

   def textBetween(start: PiecePos, end: PiecePos): String =
      if start.line == end.line then
         lineCache(start.line).substring(start.ch, end.ch)
      else
         val sb = new StringBuilder()
            .append(lineCache(start.line).substring(start.ch))
            .append('\n')

         for line <- start.line + 1 until end.line do
            sb.append(lineCache(line)).append('\n')

         sb.append(lineCache(end.line).substring(0, end.ch)).toString

   def insert(content: String, pos: PiecePos): Unit =
      if content.isEmpty then
         return

      val findResult = findPieceByLine(pos)

      if findResult.piece.isEmpty && pieceHead.isDefined then
         return

      if recording then
         pending += Edit.Insert(Position(pos.line, pos.ch), addBuffer.length, content.length)

      if findResult.piece.isEmpty then
         pieceHead = Some(new Piece(addBuffer.length, content.length(), Source.Add, None))
         addBuffer += content
         patchLinesForInsert(content, pos)
         return

      val piece = findResult.piece.get
      val nextPiece = new Piece(
         piece.offset + findResult.offset,
         piece.length - findResult.offset,
         piece.source,
         piece.next
      )
      val currentPiece = new Piece(
         addBuffer.length,
         content.length,
         Source.Add,
         Some(nextPiece)
      )

      addBuffer += content
      piece.length = findResult.offset

      if findResult.previous.isDefined && piece.length == 0 then
         val previous = findResult.previous.get
         previous.next = Some(currentPiece)
      else if piece.length == 0 then
         pieceHead = Some(currentPiece)
      else
         piece.next = Some(currentPiece)

      patchLinesForInsert(content, pos)

   def delete(pos: PiecePos): Unit =
      if pos.line == 0 && pos.ch == 0 then
         return

      val (from, text) =
         if pos.ch > 0 then 
            (Position(pos.line, pos.ch - 1), lineCache(pos.line)(pos.ch - 1).toString)
         else 
            (Position(pos.line - 1, lineCache(pos.line - 1).length), "\n")

      val findResult = findPieceByLine(pos)
      val offset = findResult.offset
      val maybePiece = findResult.piece
      val maybePrevious = findResult.previous

      if maybePiece.isEmpty || (offset == 0 && maybePrevious.isEmpty) then
         return

      if recording then 
         pending += Edit.Delete(from, text)

      val piece = maybePiece.get

      if offset == 0 then
         removeLastCharOfPiece(maybePrevious.get)
         patchLinesForDelete(pos)
         return

      if piece.length == 1 && offset == 1 then
         removePiece(piece)
         patchLinesForDelete(pos)
         return

      if offset == 1 && piece.length > 0 then
         piece.length -= 1
         piece.offset += 1
         patchLinesForDelete(pos)
         return

      if offset == piece.length - 1 then
         piece.length -= 1
         patchLinesForDelete(pos)
         return

      val newPiece = new Piece(
         piece.offset + offset,
         piece.length - offset,
         piece.source,
         piece.next
      )

      piece.next = Some(newPiece)
      piece.length = offset - 1
      patchLinesForDelete(pos)

   def deleteRange(start: PiecePos, end: PiecePos): Unit = 
      if start == end then
         return

      val startResult = findPieceByLine(start)
      val endResult = findPieceByLine(end)

      if startResult.piece.isEmpty || endResult.piece.isEmpty then
         return

      if recording then 
         pending += Edit.Delete(Position(start.line, start.ch), textBetween(start, end))

      val startPiece = startResult.piece.get
      val endPiece = endResult.piece.get

      if startPiece == endPiece then
         if startResult.offset == 0 then
            endPiece.offset += endResult.offset
            endPiece.length -= endResult.offset
            patchLinesForDeleteRange(start, end)
            return

         if endResult.offset == startPiece.length then
            startPiece.length = startResult.offset
            patchLinesForDeleteRange(start, end)
            return

         startPiece.next = Some(new Piece(
            startPiece.offset + endResult.offset,
            startPiece.length - endResult.offset,
            startPiece.source,
            startPiece.next
         ))
         startPiece.length = startResult.offset

         patchLinesForDeleteRange(start, end)
         return

      startPiece.length = startResult.offset

      endPiece.offset += endResult.offset
      endPiece.length -= endResult.offset

      var maybePiece = startPiece.next

      while maybePiece.isDefined && maybePiece.get != endPiece do
         val piece = maybePiece.get

         startPiece.next = piece.next
         maybePiece = piece.next

      if startPiece.length == 0 then
         removePiece(startPiece)

      if endPiece.length == 0 then
         removePiece(endPiece)

      patchLinesForDeleteRange(start, end)

   def length: Int = 
      var totalLength = 0
      var maybePiece = pieceHead

      while maybePiece.isDefined do
         val piece = maybePiece.get
         totalLength += piece.length
         maybePiece = piece.next
            
      totalLength

   def lineCount: Int = lineCache.size

   private def computeLines(): ArrayBuffer[String] =
      var maybeHead = pieceHead
      var line = ""
      val result = new ArrayBuffer[String]()

      while maybeHead.isDefined do
         val head = maybeHead.get
         val source = head.source match
            case Source.Original => originalBuffer
            case Source.Add     => addBuffer

         val pieceContent = source.substring(
            head.offset,
            head.offset + head.length
         )

         for c <- pieceContent do c match
            case '\n' =>
               if line.nonEmpty && line.last == '\r' then
                  line = line.dropRight(1)
               result.addOne(line)
               line = ""

            case other =>
               line = line.appended(other)

         maybeHead = head.next

      if line.nonEmpty then
         if line.last == '\r' then
            line = line.dropRight(1)
         result.addOne(line)
      else if result.isEmpty then
         result.addOne(line)

      result

   private def patchLinesForInsert(content: String, pos: PiecePos): Unit =
      val line = lineCache(pos.line)
      val parts = content.split("\n", -1)

      if parts.length == 1 then
         lineCache.update(pos.line, line.substring(0, pos.ch) + content + line.substring(pos.ch))
      else
         lineCache.update(pos.line, line.substring(0, pos.ch) + parts.head)
         lineCache.insertAll(pos.line + 1, parts.slice(1, parts.length - 1))
         lineCache.insert(pos.line + parts.length - 1, parts.last + line.substring(pos.ch))

   private def patchLinesForDelete(pos: PiecePos): Unit =
      if pos.ch > 0 then
         val line = lineCache(pos.line)
         lineCache.update(pos.line, line.substring(0, pos.ch - 1) + line.substring(pos.ch))
      else if pos.line > 0 then
         val prevLine = lineCache(pos.line - 1)
         val curLine = lineCache.remove(pos.line)
         lineCache.update(pos.line - 1, prevLine + curLine)

   private def patchLinesForDeleteRange(start: PiecePos, end: PiecePos): Unit =
      if start.line == end.line then
         val line = lineCache(start.line)
         lineCache.update(start.line, line.substring(0, start.ch) + line.substring(end.ch))
      else
         val startLine = lineCache(start.line)
         val endLine = lineCache(end.line)
         lineCache.update(start.line, startLine.substring(0, start.ch) + endLine.substring(end.ch))
         lineCache.remove(start.line + 1, end.line - start.line)

   private def findPieceByLine(pos: PiecePos): FindResult = boundary:
      var maybeHead = pieceHead
      var previous: Option[Piece] = None
      var currentLine = 0
      var currentCharacter = 0

      while maybeHead.isDefined do
         val head = maybeHead.get
         val source = head.source match
            case Source.Original => originalBuffer
            case Source.Add     => addBuffer

         val content = source.substring(
            head.offset,
            head.offset + head.length,
         )

         for i <- 0 until content.length do
            val letter = content.charAt(i)

            if currentLine == pos.line && currentCharacter == pos.ch then
               break(new FindResult(Some(head), previous, i))

            if letter == '\n' then
               currentLine += 1
               currentCharacter = 0
            else if letter != '\r' then
               currentCharacter += 1

         if head.next.isEmpty then
            break(new FindResult(Some(head), previous, head.length))

         previous = Some(head)
         maybeHead = head.next

      new FindResult(None, None, 0)

   private def removeLastCharOfPiece(piece: Piece): Unit =
      piece.length -= 1

      if piece.length == 0 then
         removePiece(piece)

   private def removePiece(piece: Piece): Unit =
      var maybeHead = pieceHead
      var maybePrevious: Option[Piece] = None

      while maybeHead.isDefined do
         val head = maybeHead.get
         if head == piece then
            if maybePrevious.isDefined then
               val previous = maybePrevious.get
               previous.next = head.next
            else 
               pieceHead = head.next
            
            return

         maybePrevious = Some(head)
         maybeHead = head.next

enum Edit:
   case Insert(at: Position, addStart: Int, length: Int)
   case Delete(from: Position, text: String)

class Piece(
   var offset: Int,
   var length: Int,
   var source: Source,
   var next: Option[Piece],
)

enum Source:
   case Original
   case Add

case class FindResult(
   piece: Option[Piece],
   previous: Option[Piece],
   offset: Int,
)

case class PieceOffset(piece: Piece, offset: Int)

case class PiecePos(line: Int, ch: Int)