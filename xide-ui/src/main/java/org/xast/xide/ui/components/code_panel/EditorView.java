package org.xast.xide.ui.components.code_panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.KeyStroke;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.FileSaveRequestedEvent;
import org.xast.xide.core.plugin.file.FileModel;
import org.xast.xide.core.plugin.file.TextFileModel;
import org.xast.xide.core.plugin.ui.CodePanelView;
import org.xast.xide.core.plugin.ui.StatusLabel;
import org.xast.xide.core.utils.Debug;
import org.xast.xide.ui.lsp.LspClient;
import org.xast.xide.ui.lsp.LspCompletionProvider;
import org.xast.xide.ui.utils.SyntaxStyle;
import org.xast.xide.ui.utils.XideStyle;

import lombok.AllArgsConstructor;

public class EditorView extends CodePanelView {
    private static final long LSP_SYNC_DELAY_MILLIS = 150;

    private RSyntaxTextArea textArea;
    private RTextScrollPane scrollPane;
    private final AtomicInteger documentVersion;
    private final String fileUri;
    private final SyntaxStyle syntaxStyle;
    private final ScheduledExecutorService lspSyncExecutor;
    private final StatusLabel statusLabel;
    private Optional<LspClient> lspClient;
    private LspCompletionProvider lspCompletionProvider;
    private volatile ScheduledFuture<?> pendingLspSync;
    private volatile ScheduledFuture<?> pendingLspPrewarm;

    public EditorView(
        EventBus eventBus, 
        File file, 
        SyntaxStyle syntaxStyle, 
        StatusLabel statusLabel,
        int tabSize
    ) {
        this(eventBus, file, syntaxStyle, tabSize, statusLabel, Optional.empty());
    }

    public EditorView(
        EventBus eventBus, 
        File file, 
        SyntaxStyle syntaxStyle, 
        int tabSize,
        StatusLabel statusLabel,
        Optional<LspClient> lspClient
    ) {
        super();
        this.statusLabel = statusLabel;
        this.documentVersion = new AtomicInteger(1);
        this.fileUri = file.toURI().toString();
        this.syntaxStyle = syntaxStyle;
        this.lspSyncExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "xide-lsp-sync");
            thread.setDaemon(true);
            return thread;
        });
        this.lspClient = Optional.empty();
        this.lspCompletionProvider = null;
        this.pendingLspSync = null;
        this.pendingLspPrewarm = null;

        XideStyle style = XideStyle.getCurrent();
        Font font = style.codeFont().deriveFont(18f);
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

        try {
            Theme theme = Theme.load(
                getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
                )
            );

            theme.apply(textArea);
        } catch (IOException e) {
            Debug.error("Failed to load editor theme: " + e.getMessage());
        }

        textArea.setSyntaxEditingStyle(syntaxStyle.getMimeType());
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setTabsEmulated(true);
        textArea.setTabSize(tabSize);

        configureLsp(lspClient);

        textArea.getDocument().addDocumentListener(
            new EditorDocumentListener(
                eventBus,
                file
            )
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
        scrollPane.setBackground(bgColor);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

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

    public void configureLsp(Optional<LspClient> lspClient) {
        if (this.lspClient.isPresent() || lspClient.isEmpty()) {
            return;
        }

        this.lspClient = lspClient;
        updateLspState(true, false);
        setupLspCompletion(lspClient.get(), fileUri);
        openDocument(lspClient.get(), textArea.getText());
        scheduleLspPrewarm();
    }

    private void setupLspCompletion(LspClient lspClient, String fileUri) {
        try {
            lspCompletionProvider = new LspCompletionProvider(
                lspClient,
                textArea,
                fileUri,
                this::updateLspState
            );
            AutoCompletion ac = new AutoCompletion(lspCompletionProvider);
            ac.install(textArea);
            KeyStroke triggerKey = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK);
            String actionKey = "xide.lsp.completion";
            textArea.getInputMap().put(triggerKey, actionKey);
            textArea.getActionMap().put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!lspCompletionProvider.isReady()) {
                        if (!lspCompletionProvider.isLoading()) {
                            lspCompletionProvider.warmUp();
                        }
                        return;
                    }

                    lspCompletionProvider.requestCompletions(ac::doCompletion);
                }
            });
            ac.setAutoCompleteSingleChoices(false);
        } catch (Exception e) {
            Debug.error("Failed to setup LSP completion: " + e.getMessage());
        }
    }

    private void openDocument(LspClient lspClient, String content) {
        try {
            lspClient.openDocument(fileUri, syntaxStyle.name().toLowerCase(), content);
        } catch (RuntimeException e) {
            Debug.error("Failed to open LSP document: " + e.getMessage());
        }
    }

    private void scheduleLspSync(String text, int version) {
        ScheduledFuture<?> pendingSync = pendingLspSync;
        if (pendingSync != null) {
            pendingSync.cancel(false);
        }

        pendingLspSync = lspSyncExecutor.schedule(
            () -> sendLspChange(text, version),
            LSP_SYNC_DELAY_MILLIS,
            TimeUnit.MILLISECONDS
        );
    }

    private void scheduleLspPrewarm() {
        if (lspCompletionProvider == null) {
            return;
        }

        ScheduledFuture<?> existingPrewarm = pendingLspPrewarm;
        if (existingPrewarm != null) {
            existingPrewarm.cancel(false);
        }

        pendingLspPrewarm = lspSyncExecutor.schedule(
            () -> lspCompletionProvider.warmUp(),
            300,
            TimeUnit.MILLISECONDS
        );
    }

    private void sendLspChange(String text, int version) {
        if (lspClient.isEmpty()) {
            return;
        }

        try {
            lspClient.get().changeDocument(fileUri, version, text);
        } catch (RuntimeException e) {
            Debug.error("Failed to update LSP document: " + e.getMessage());
        }
    }

    @AllArgsConstructor
    private class EditorDocumentListener implements DocumentListener {
        private EventBus eventBus;
        private File file;

        private void handleUpdate() {
            eventBus.publish(new FileSaveRequestedEvent(file, false));

            if (lspClient.isEmpty()) {
                return;
            }

            try {
                if (lspCompletionProvider != null) {
                    lspCompletionProvider.clearCompletions();
                }

                int version = documentVersion.incrementAndGet();
                scheduleLspSync(textArea.getText(), version);
            } catch (RuntimeException e) {
                Debug.error("Failed to queue LSP document update: " + e.getMessage());
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            handleUpdate();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            handleUpdate();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {}
    }

    @Override
    public void close() {
        ScheduledFuture<?> pendingSync = pendingLspSync;
        if (pendingSync != null) {
            pendingSync.cancel(false);
        }

        ScheduledFuture<?> pendingPrewarm = pendingLspPrewarm;
        if (pendingPrewarm != null) {
            pendingPrewarm.cancel(false);
        }

        lspSyncExecutor.shutdownNow();

        if (lspClient.isEmpty()) {
            return;
        }

        try {
            lspClient.get().closeDocument(fileUri);
        } catch (RuntimeException e) {
            Debug.error("Failed to close LSP document: " + e.getMessage());
        }
    }

    private void updateLspState(boolean loading, boolean ready) {
        if (ready) {
            statusLabel.setValue("LSP: ready");
        } else if (loading) {
            statusLabel.setValue("LSP: loading...");
        } else if (lspClient.isPresent()) {
            statusLabel.setValue("LSP: idle");
        } else {
            statusLabel.setValue("LSP: off");
        }

        if (loading) {
            textArea.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }
    }
}
