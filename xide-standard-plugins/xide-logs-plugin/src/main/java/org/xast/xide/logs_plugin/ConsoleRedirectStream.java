package org.xast.xide.logs_plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ConsoleRedirectStream extends OutputStream {
    private final LogsView view;
    private final boolean error;

    private final StringBuilder buffer = new StringBuilder();

    public ConsoleRedirectStream(LogsView view, boolean error) {
        this.view = view;
        this.error = error;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        String text = new String(b, off, len, StandardCharsets.UTF_8);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                view.addLine(buffer.toString(), error);
                buffer.setLength(0);
            } else if (c != '\r') {
                buffer.append(c);
            }
        }
    }
}