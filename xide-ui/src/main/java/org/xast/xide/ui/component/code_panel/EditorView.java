package org.xast.xide.ui.component.code_panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Files;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.xast.xide.ui.utils.XideStyle;

public class EditorView extends JPanel {
    private RSyntaxTextArea textArea;
    private RTextScrollPane scrollPane;
    private File file;
    private boolean dirty = false;

    public EditorView(File file) {
        super(new BorderLayout());

        XideStyle style = XideStyle.getCurrent();
        Font font = style.codeFont().deriveFont(16f);
        String content = new String();

        if (file.exists() && file.canRead()) {
            try {
                content = Files.readString(file.toPath());
            } catch (Exception e) { 
                e.printStackTrace(); 
            }
        } else {
            dirty = true;
        }

        // Create the text area
        textArea = new RSyntaxTextArea(content);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);

        // Set IDE theme
        Color bgColor = UIManager.getColor("TextArea.background");
        textArea.setBackground(bgColor);
        textArea.setForeground(UIManager.getColor("TextArea.foreground"));
        textArea.setCaretColor(UIManager.getColor("TextArea.caretForeground"));
        textArea.setSelectionColor(XideStyle.lighten(bgColor, 0.2f));
        textArea.setHighlightCurrentLine(true);
        textArea.setCurrentLineHighlightColor(XideStyle.lighten(bgColor, 0.1f));
        textArea.setFont(font);

        // Scroll pane
        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);

        var gutter = scrollPane.getGutter();
        gutter.setLineNumberFont(font);
        gutter.setSpacingBetweenLineNumbersAndFoldIndicator(8);

        this.add(scrollPane, BorderLayout.CENTER);
    }

    public void setSyntaxEditingStyle(String style) {
        textArea.setSyntaxEditingStyle(style);
    }
}
