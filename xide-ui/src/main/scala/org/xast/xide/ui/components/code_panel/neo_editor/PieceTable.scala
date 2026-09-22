package org.xast.xide.ui.components.code_panel.neo_editor

import scala.collection.mutable.ArrayBuffer

class PieceTable(val content: String):

    private val originalBuffer: String   = content
    private var addBuffer: String        = ""
    private var pieceHead: Option[Piece] = Some(new Piece(0, content.length, Source.Original, None))

    /**
      * Renders full piece table into lines (ArrayBuffer of Strings).
      * 
      * Do not use it for rendering text into editor - only for
      * caching and saving content to a file.
      */
    def read(): ArrayBuffer[String] =
        var maybeHead = pieceHead
        var line = ""
        val lines = new ArrayBuffer[String]()

        while maybeHead.isDefined do
            val head = maybeHead.get
            val source = head.source match
                case Source.Original => originalBuffer 
                case Source.Add      => addBuffer

            val content = source.substring(
                head.offset, 
                head.offset + head.length
            )

            for c <- content do c match
                case '\n' =>
                    lines.addOne(line)
                    line = ""

                case other =>
                    line = line.appended(other)

            maybeHead = head.next

        if (!line.isEmpty()) {
            lines.addOne(line)
        }

        return lines

    def insert(content: String, pos: Position): Unit =
        val findResult = findPieceByLine(pos)

        if findResult.piece.isEmpty then
            if pieceHead.isEmpty then
                addBuffer += content
                pieceHead = Some(new Piece(0, content.length(), Source.Add, None))
                return
            
            return

        var piece = findResult.piece.get
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
            var previous = findResult.previous.get
            previous.next = Some(currentPiece)
        else if piece.length == 0 then
            pieceHead = Some(currentPiece)
        else
            piece.next = Some(currentPiece)
        

    def delete(pos: Position): Unit =
        val findResult = findPieceByLine(pos)
        val offset = findResult.offset
        var maybePiece = findResult.piece
        var maybePrevious = findResult.previous

        if maybePiece.isEmpty then
            return

        var piece = maybePiece.get

        if offset == 0 && maybePrevious.isDefined then
            val previous = maybePrevious.get
            removeLastCharOfPiece(previous)
            return
        else 
            if offset == 0 then
                return

        if piece.length == 1 && offset == 1 then
            removePiece(piece)
            return

        if offset == 1 && piece.length > 0 then
            piece.length -= 1
            piece.offset += 1
            return

        if offset == piece.length - 1 then
            piece.length -= 1
            return

        val newPiece = new Piece(
            piece.offset + offset,
            piece.length - offset,
            piece.source,
            piece.next
        )

        piece.next = Some(newPiece)
        piece.length = offset - 1

    def deleteRange(start: Position, end: Position): Unit = 
        val startResult = findPieceByLine(start)
        val endResult = findPieceByLine(end)

        if startResult.piece.isEmpty || endResult.piece.isEmpty then
            return

        var startPiece = startResult.piece.get
        var endPiece = endResult.piece.get

        if startPiece == endPiece then
            if startResult.offset == 0 then
                endPiece.offset += endResult.offset
                endPiece.length -= endResult.offset
                return

            if endResult.offset == startPiece.length then
                startPiece.length = startResult.offset
                return

            startPiece.next = Some(new Piece(
                startPiece.offset + endResult.offset,
                startPiece.length - endResult.offset,
                startPiece.source,
                startPiece.next
            ))
            startPiece.length = startResult.offset

            return

        startPiece.length = startResult.offset

        endPiece.offset += endResult.offset
        endPiece.length -= endResult.offset

        var maybePiece = startPiece.next

        while maybePiece.isDefined && maybePiece.get == endPiece do
            val piece = maybePiece.get

            startPiece.next = piece.next
            maybePiece = piece.next

        if startPiece.length == 0 then
            removePiece(startPiece)

        if endPiece.length == 0 then
            removePiece(endPiece)

    def length: Int = 
        var totalLength = 0
        var maybePiece = pieceHead

        while maybePiece.isDefined do
            val piece = maybePiece.get
            totalLength += piece.length
            maybePiece = piece.next
                
        totalLength

    def forEachChar(
        startOffset: Int, 
        endOffset: Int, 
        consume: (ch: Char, globalOffset: Int) => Unit,
    ): Unit = 
        val contentLength = this.length
        val start = Math.max(0, Math.min(startOffset, contentLength))
        val end = Math.max(0, Math.min(endOffset, contentLength))

        if start >= end then 
            return

        var maybePiece = pieceHead
        var pieceStartOffset = 0

        while maybePiece.isDefined && pieceStartOffset < end do
            val piece = maybePiece.get
            val pieceEndOffset = pieceStartOffset + piece.length

            if pieceEndOffset > start then
                val fromInPiece = Math.max(start, pieceStartOffset) - pieceStartOffset
                val toInPiece = Math.min(end, pieceEndOffset) - pieceStartOffset
                val source = piece.source match
                    case Source.Original => originalBuffer 
                    case Source.Add      => addBuffer

                for i <- fromInPiece until toInPiece do consume(
                    source.charAt(piece.offset + i), 
                    pieceStartOffset + i,
                )

            pieceStartOffset = pieceEndOffset
            maybePiece = piece.next

    def forEachLine(
        startLine: Int, 
        endLine: Int, 
        consume: (line: Int, startOffset: Int, endOffset: Int) => Unit,
    ): Unit = 
        val fromLine = Math.max(0, startLine)
        val toLine = Math.max(fromLine, endLine)
        val contentLength = this.length
        var currentLine = 0
        var lineStartOffset = 0

        forEachChar(0, contentLength, (ch, globalOffset) =>
            if ch != '\n' then
                return

            if currentLine >= fromLine && currentLine < toLine then
                consume(currentLine, lineStartOffset, globalOffset)

            currentLine += 1
            lineStartOffset = globalOffset + 1
        )

        if currentLine >= fromLine && currentLine < toLine then
            consume(currentLine, lineStartOffset, contentLength)

    def lineCount(): Int =
        var count = 1
        var maybePiece = pieceHead

        while maybePiece.isDefined do
            val piece = maybePiece.get
            val source = piece.source match
                case Source.Original => originalBuffer          
                case Source.Add      => addBuffer

            val content = source.substring(
                piece.offset,
                piece.offset + piece.length,
            )

            for i <- 0 until content.length do
                if content.charAt(i) == '\n' then
                    count += 1
                
            maybePiece = piece.next

        return count

    // def List<String> readLines(int startLine, int endLine) {
    //     List<String> result = new ArrayList<>()
    //     StringBuilder current = new StringBuilder()
    //     int line = 0
    //     boolean done = false
    //     Optional<Piece> maybePiece = pieceHead

    //     while (maybePiece.isPresent() && !done) {
    //         Piece piece = maybePiece.get()
    //         String source = piece.getSource() == Source.ORIGINAL
    //             ? originalBuffer
    //             : addBuffer
    //         String content = source.substring(
    //             piece.getOffset(),
    //             piece.getOffset() + piece.getLength()
    //         )

    //         for (int i = 0 i < content.length() i++) {
    //             char ch = content.charAt(i)

    //             if (ch == '\n') {
    //                 if (line >= startLine && line < endLine) {
    //                     result.add(current.toString())
    //                 }
    //                 current.setLength(0)
    //                 line++
    //                 if (line >= endLine) {
    //                     done = true
    //                     break
    //                 }
    //                 continue
    //             }

    //             if (line >= startLine) {
    //                 current.append(ch)
    //             }
    //         }

    //         maybePiece = piece.next
    //     }

    //     if (!done && line >= startLine && line < endLine) {
    //         result.add(current.toString())
    //     }

    //     return result
    // }

    private def findPieceByLine(pos: Position): FindResult =
        var maybeHead = pieceHead
        var previous: Option[Piece] = None
        var currentLine = 0
        var currentCharacter = 0

        while maybeHead.isDefined do
            val head = maybeHead.get
            val source = head.source match
                case Source.Original => originalBuffer
                case Source.Add      => addBuffer
                
            val content = source.substring(
                head.offset, 
                head.offset + head.length,
            )

            for i <- 0 until content.length do
                val letter = content.charAt(i)

                if currentLine == pos.line && currentCharacter == pos.ch then
                    return new FindResult(Some(head), previous, i)

                if letter == '\n' then
                    currentLine += 1
                    currentCharacter = 0
                else
                    currentCharacter += 1

            if head.next.isEmpty then
                return new FindResult(Some(head), previous, head.length)

            previous = Some(head)
            maybeHead = head.next

        return new FindResult(None, None, 0)

    private def removeLastCharOfPiece(piece: Piece): Unit =
        piece.length -= 1

        if (piece.length == 0) then
            removePiece(piece)

    private def removePiece(piece: Piece): Unit =
        var maybeHead = pieceHead
        var maybePrevious: Option[Piece] = None

        while maybeHead.isDefined do
            val head = maybeHead.get
            if head == piece then
                if maybePrevious.isDefined then
                    var previous = maybePrevious.get
                    previous.next = head.next
                else 
                    pieceHead = head.next
                
                return

            maybePrevious = Some(head)
            maybeHead = head.next

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

case class Position(line: Int, ch: Int)