package org.xast.xide.ui.components.code_panel.neo_editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Data;

public class PieceTable {
    @FunctionalInterface
    public interface CharOffsetConsumer {
        void accept(char ch, int globalOffset);
    }

    @FunctionalInterface
    public interface LineConsumer {
        void accept(int line, int startOffset, int endOffset);
    }

    private String originalBuffer;
    private String addBuffer;
    private Optional<Piece> pieceHead;

    public PieceTable(String content) {
        originalBuffer = content;
        addBuffer = new String();
        pieceHead = Optional.of(
            new Piece(0, content.length(), Source.ORIGINAL, Optional.empty())
        );
    }

    public List<String> read() {
        Optional<Piece> maybeHead = this.pieceHead;
        String line = "";
        List<String> lines = new ArrayList<>();

        while (maybeHead.isPresent()) {
            Piece head = maybeHead.get();
            String source = head.getSource() == Source.ORIGINAL
                ? this.originalBuffer
                : this.addBuffer;

            String content = source.substring(
                head.getOffset(), 
                head.getOffset() + head.getLength()
            );

            for (int i = 0; i < content.length(); i++) {
                char letter = content.charAt(i);
                if (letter == '\n') {
                    lines.add(line);
                    line = "";
                    continue;
                }

                line += letter;
            }

            maybeHead = head.next;
        }

        if (!line.isEmpty()) {
            lines.add(line);
        }

        return lines;
    }

    public void insert(String content, Position pos) {
        FindResult findResult = findPieceByLine(pos);
        Optional<Piece> maybePiece = findResult.piece;
        Optional<Piece> maybePrevious = findResult.previous;
        int offset = findResult.offset;

        if (maybePiece.isEmpty()) {
            if (pieceHead.isEmpty()) {
                addBuffer += content;
                pieceHead = Optional.of(
                    new Piece(0, content.length(), Source.ADD, null)
                );
                return;
            }
            return;
        }

        Piece piece = maybePiece.get();

        Piece nextPiece = new Piece(
            piece.offset + offset,
            piece.length - offset,
            piece.source,
            piece.next
        );

        Piece currentPiece = new Piece(
            addBuffer.length(),
            content.length(),
            Source.ADD,
            Optional.of(nextPiece)
        );

        addBuffer += content;
        piece.length = offset;

        if (maybePrevious.isPresent() && piece.length == 0) {
            Piece previous = maybePrevious.get();
            previous.next = Optional.of(currentPiece);
        } else if (piece.length == 0) {
            pieceHead = Optional.of(currentPiece);
        } else {
            piece.next = Optional.of(currentPiece);
        }
    }

    private FindResult findPieceByLine(Position position) {
        Optional<Piece> maybeHead = pieceHead;
        Optional<Piece> previous = Optional.empty();
        int line = position.line;
        int character = position.ch;
        int currentLine = 0;
        int currentCharacter = 0;

        while (maybeHead.isPresent()) {
            Piece head = maybeHead.get();
            String source = head.source == Source.ORIGINAL 
                ? originalBuffer 
                : addBuffer;
                
            String content = source.substring(head.offset, head.offset + head.length);

            for (int i = 0; i < content.length(); i++) {
                char letter = content.charAt(i);

                if (currentLine == line && currentCharacter == character) {
                    return new FindResult(Optional.of(head), previous, i);
                }

                if (letter == '\n') {
                    currentLine++;
                    currentCharacter = 0;
                } else {
                    currentCharacter++;
                }
            }

            if (head.next.isEmpty()) {
                return new FindResult(Optional.of(head), previous, head.length);
            }

            previous = Optional.of(head);
            maybeHead = head.next;
        }

        return new FindResult(Optional.empty(), Optional.empty(), 0);
    }

    public void delete(Position pos) {
        FindResult findResult = findPieceByLine(pos);
        Optional<Piece> maybePiece = findResult.piece;
        Optional<Piece> maybePrevious = findResult.previous;
        int offset = findResult.offset;

        if (maybePiece.isEmpty()) {
            return;
        }

        Piece piece = maybePiece.get();

        if (offset == 0 && maybePrevious.isPresent()) {
            Piece previous = maybePrevious.get();
            removeLastCharacterOfPiece(previous);
            return;
        } else if (offset == 0) {
            return;
        }

        if (piece.length == 1 && offset == 1) {
            removePiece(piece);
            return;
        }

        if (offset == 1 && piece.length > 0) {
            piece.length--;
            piece.offset++;
            return;
        }

        if (offset == piece.length - 1) {
            piece.length--;
            return;
        }

        Piece newPiece = new Piece(
            piece.offset + offset,
            piece.length - offset,
            piece.source,
            piece.next
        );

        piece.next = Optional.of(newPiece);
        piece.length = offset - 1;
    }

    private void removeLastCharacterOfPiece(Piece piece) {
        piece.length--;

        if (piece.length == 0) {
            removePiece(piece);
        }
    }

    private void removePiece(Piece piece) {
        Optional<Piece> maybeHead = this.pieceHead;
        Optional<Piece> maybePrevious = Optional.empty();

        while (maybeHead.isPresent()) {
            Piece head = maybeHead.get();
            if (head.equals(piece)) {
                if (maybePrevious.isPresent()) {
                    Piece previous = maybePrevious.get();
                    previous.next = head.next;
                } else {
                    this.pieceHead = head.next;
                }
                return;
            }

            maybePrevious = Optional.of(head);
            maybeHead = head.next;
        }
    }

    public void deleteRange(Position start, Position end) {
        FindResult startResult = findPieceByLine(start);
        Optional<Piece> maybeStartPiece = startResult.piece;
        int startOffset = startResult.offset;

        FindResult endResult = findPieceByLine(end);
        Optional<Piece> maybeEndPiece = endResult.piece;
        int endOffset = endResult.offset;

        if (maybeStartPiece.isEmpty() || maybeEndPiece.isEmpty()) {
            return;
        }

        Piece startPiece = maybeStartPiece.get();
        Piece endPiece = maybeEndPiece.get();

        if (startPiece.equals(endPiece)) {
            if (startOffset == 0) {
                endPiece.offset += endOffset;
                endPiece.length -= endOffset;
                return;
            }

            if (endOffset == startPiece.length) {
                startPiece.length = startOffset;
                return;
            }

            startPiece.next = Optional.of(new Piece(
                startPiece.offset + endOffset,
                startPiece.length - endOffset,
                startPiece.source,
                startPiece.next
            ));
            startPiece.length = startOffset;

            return;
        }

        startPiece.length = startOffset;

        endPiece.offset += endOffset;
        endPiece.length -= endOffset;

        Optional<Piece> maybePiece = startPiece.next;

        while (maybePiece.isPresent()) {
            Piece piece = maybePiece.get();

            if (piece.equals(endPiece)) {
                break;
            }

            startPiece.next = piece.next;
            maybePiece = piece.next;
        }

        if (startPiece.length == 0) {
            removePiece(startPiece);
        }

        if (endPiece.length == 0) {
            removePiece(endPiece);
        }
    }

    public int length() {
        int totalLength = 0;
        Optional<Piece> maybePiece = pieceHead;

        while (maybePiece.isPresent()) {
            Piece piece = maybePiece.get();
            totalLength += piece.length;
            maybePiece = piece.next;
        }

        return totalLength;
    }

    public void forEachChar(int startOffset, int endOffset, CharOffsetConsumer consumer) {
        int contentLength = length();
        int start = Math.max(0, Math.min(startOffset, contentLength));
        int end = Math.max(0, Math.min(endOffset, contentLength));

        if (start >= end) {
            return;
        }

        Optional<Piece> maybePiece = pieceHead;
        int pieceStartOffset = 0;

        while (maybePiece.isPresent() && pieceStartOffset < end) {
            Piece piece = maybePiece.get();
            int pieceEndOffset = pieceStartOffset + piece.length;

            if (pieceEndOffset > start) {
                int fromInPiece = Math.max(start, pieceStartOffset) - pieceStartOffset;
                int toInPiece = Math.min(end, pieceEndOffset) - pieceStartOffset;
                String source = piece.source == Source.ORIGINAL ? originalBuffer : addBuffer;

                for (int i = fromInPiece; i < toInPiece; i++) {
                    consumer.accept(source.charAt(piece.offset + i), pieceStartOffset + i);
                }
            }

            pieceStartOffset = pieceEndOffset;
            maybePiece = piece.next;
        }
    }

    public void forEachLine(int startLine, int endLine, LineConsumer consumer) {
        int fromLine = Math.max(0, startLine);
        int toLine = Math.max(fromLine, endLine);
        int contentLength = length();
        int[] currentLine = new int[] {0};
        int[] lineStartOffset = new int[] {0};

        forEachChar(0, contentLength, (ch, globalOffset) -> {
            if (ch != '\n') {
                return;
            }

            if (currentLine[0] >= fromLine && currentLine[0] < toLine) {
                consumer.accept(currentLine[0], lineStartOffset[0], globalOffset);
            }

            currentLine[0]++;
            lineStartOffset[0] = globalOffset + 1;
        });

        if (currentLine[0] >= fromLine && currentLine[0] < toLine) {
            consumer.accept(currentLine[0], lineStartOffset[0], contentLength);
        }
    }

    public static record PieceOffset(Piece piece, int offset) {}

    public static record Position(int line, int ch) {}

    private static enum Source {
        ORIGINAL,
        ADD,
    }

    private static record FindResult(
        Optional<Piece> piece,
        Optional<Piece> previous,
        int offset
    ) {}

    @Data
    @AllArgsConstructor
    private static class Piece {
        private int offset;
        private int length;
        private Source source;
        private Optional<Piece> next;
    }
}