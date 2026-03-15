package org.xast.xide.ui.components.code_panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.FileSaveRequestedEvent;
import org.xast.xide.core.plugin.file.FileModel;
import org.xast.xide.core.plugin.file.TextFileModel;
import org.xast.xide.core.plugin.ui.CodePanelView;
import org.xast.xide.core.utils.Debug;
import org.xast.xide.ui.utils.SyntaxStyle;
import org.xast.xide.ui.utils.XideStyle;

import lombok.AllArgsConstructor;

public class EditorView extends CodePanelView {
    private RSyntaxTextArea textArea;
    private RTextScrollPane scrollPane;

    public EditorView(
        EventBus eventBus, 
        File file, 
        SyntaxStyle syntaxStyle, 
        int tabSize
    ) {
        super();

        XideStyle style = XideStyle.getCurrent();
        Font font = style.codeFont().deriveFont(16f);
        String content = new String();

        if (file.exists() && file.canRead()) {
            try {
                content = Files.readString(file.toPath());
            } catch (IOException e) { 
                Debug.error("Cannot read file `"+file.getName()+"`: "+e.getMessage());
            }
        }

        // Create the text area
        textArea = new RSyntaxTextArea(content);
        textArea.setSyntaxEditingStyle(syntaxStyle.getMimeType());
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setTabsEmulated(true);
        textArea.setTabSize(tabSize);
        textArea.getDocument().addDocumentListener(
            new EditorDocumentListener(eventBus, file)
        );

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

    @Override
    public FileModel model() {
        return new TextFileModel(textArea.getText());
    }

    public void setSyntaxEditingStyle(String style) {
        textArea.setSyntaxEditingStyle(style);
    }

    @AllArgsConstructor
    private static class EditorDocumentListener implements DocumentListener {
        private EventBus eventBus;
        private File file;

        @Override
        public void insertUpdate(DocumentEvent e) {
            eventBus.publish(new FileSaveRequestedEvent(file, false));
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            eventBus.publish(new FileSaveRequestedEvent(file, false));
        }

        @Override
        public void changedUpdate(DocumentEvent e) {}
    }
}
