package org.xast.xide.ui.components.code_panel.neo_editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PieceTable {
    private String originalBuffer;
    private String addBuffer;
    private Piece pieceHead;

    public PieceTable(String content) {
        originalBuffer = content;
        addBuffer = new String();
        pieceHead = new Piece(0, content.length(), Source.ORIGINAL, Optional.empty());
    }

    public List<String> read() {
        Optional<Piece> maybeHead = Optional.of(this.pieceHead);
        String line = "";
        List<String> lines = new ArrayList<>();

        while (maybeHead.isPresent()) {
            Piece head = maybeHead.get();
            String source = head.source() == Source.ORIGINAL
                ? this.originalBuffer
                : this.addBuffer;

            String content = source.substring(
                head.offset(), 
                head.offset() + head.length()
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

    public void insert(String content, Position pos) {}

    public void delete(Position pos) {}

    public void deleteRange(Position start, Position end) {}

    public static record Position(int number, int ch) {}

    private static enum Source {
        ORIGINAL,
        ADD,
    }

    private static record Piece(
        int offset,
        int length,
        Source source,
        Optional<Piece> next
    ) {}
}