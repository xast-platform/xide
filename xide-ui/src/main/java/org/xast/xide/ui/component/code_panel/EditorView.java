package org.xast.xide.ui.component.code_panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.xast.xide.ui.utils.XideStyle;

public class EditorView extends JPanel {
    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;

    public EditorView() {
        super(new BorderLayout());

        XideStyle style = XideStyle.getCurrent();
        Font font = style.codeFont().deriveFont(16f);

        // Create the text area
        textArea = new RSyntaxTextArea();
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
