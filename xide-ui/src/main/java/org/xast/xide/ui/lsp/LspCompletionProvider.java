package org.xast.xide.ui.lsp;

import java.awt.Point;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.fife.ui.autocomplete.AbstractCompletionProvider;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.xast.xide.core.utils.Debug;

public class LspCompletionProvider extends AbstractCompletionProvider {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final int FALLBACK_SCAN_LIMIT = 120_000;

    @FunctionalInterface
    public interface LspStateListener {
        void onStateChanged(boolean loading, boolean ready);
    }

    private final LspClient lspClient;
    private final RSyntaxTextArea textArea;
    private final String fileUri;
    private final LspStateListener stateListener;
    private final AtomicInteger requestSequence;
    private final AtomicBoolean loading;
    private final AtomicBoolean ready;
    private volatile List<Completion> cachedCompletions;
    private volatile CompletableFuture<Either<List<CompletionItem>, CompletionList>> inFlightRequest;

    public LspCompletionProvider(
        LspClient lspClient,
        RSyntaxTextArea textArea,
        String fileUri,
        LspStateListener stateListener
    ) {
        this.lspClient = lspClient;
        this.textArea = textArea;
        this.fileUri = fileUri;
        this.stateListener = stateListener;
        this.requestSequence = new AtomicInteger();
        this.loading = new AtomicBoolean(false);
        this.ready = new AtomicBoolean(false);
        this.cachedCompletions = List.of();
        this.inFlightRequest = null;
        notifyState(false, false);
    }

    @Override
    protected List<Completion> getCompletionsImpl(JTextComponent comp) {
        return new ArrayList<>(cachedCompletions);
    }

    public boolean isReady() {
        return ready.get();
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void requestCompletions(Runnable onComplete) {
        if (!ready.get()) {
            return;
        }

        String typedPrefix = getAlreadyEnteredText(textArea);
        CompletionParams params = createCompletionParams();
        if (params == null) {
            cachedCompletions = List.of();
            return;
        }

        int requestId = requestSequence.incrementAndGet();
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> previousRequest = inFlightRequest;
        if (previousRequest != null) {
            previousRequest.cancel(true);
        }

        CompletableFuture<Either<List<CompletionItem>, CompletionList>> request =
            lspClient.completionAsync(params);
        inFlightRequest = request;
        setLoading(true);

        request.thenApply(this::toCompletions)
            .exceptionally(error -> {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                if (!(cause instanceof CancellationException)) {
                    Debug.error("LSP completion error: " + cause);
                }
                return List.of();
            })
            .thenAccept(completions -> {
                if (requestSequence.get() != requestId) {
                    return;
                }

                List<Completion> merged = mergeCompletions(
                    completions,
                    localFallbackCompletions(typedPrefix)
                );
                List<Completion> filtered = filterByPrefix(merged, typedPrefix);

                setLoading(false);
                inFlightRequest = null;
                cachedCompletions = filtered;
                if (!filtered.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        onComplete.run();
                    });
                }
            });
    }

    public void warmUp() {
        if (ready.get() || loading.get()) {
            return;
        }

        CompletionParams params = new CompletionParams(
            new TextDocumentIdentifier(fileUri),
            new Position(0, 0)
        );
        setLoading(true);

        lspClient.completionAsync(params)
            .thenAccept(result -> {
                ready.set(true);
                setLoading(false);
            })
            .exceptionally(error -> {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                if (!(cause instanceof CancellationException)) {
                    Debug.error("LSP warm-up error: " + cause);
                }
                setLoading(false);
                return null;
            });
    }

    public void clearCompletions() {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> request = inFlightRequest;
        if (request != null) {
            request.cancel(true);
            inFlightRequest = null;
        }

        setLoading(false);

        cachedCompletions = List.of();
    }

    private void setLoading(boolean loading) {
        this.loading.set(loading);
        notifyState(loading, ready.get());
    }

    private void notifyState(boolean loading, boolean ready) {
        if (stateListener == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> stateListener.onStateChanged(loading, ready));
    }

    private CompletionParams createCompletionParams() {
        try {
            int offset = textArea.getCaretPosition();
            int line = textArea.getLineOfOffset(offset);
            int lineStart = textArea.getLineStartOffset(line);
            int lineEnd = textArea.getLineEndOffset(line);
            String lineText = textArea.getText(lineStart, lineEnd - lineStart);

            int logicalLineLength = lineText.length();
            while (logicalLineLength > 0) {
                char ch = lineText.charAt(logicalLineLength - 1);
                if (ch != '\n' && ch != '\r') {
                    break;
                }
                logicalLineLength--;
            }

            int rawColumn = offset - lineStart;
            int column = Math.max(0, Math.min(rawColumn, logicalLineLength));

            Position position = new Position(line, column);
            TextDocumentIdentifier docId = new TextDocumentIdentifier(fileUri);
            CompletionParams params = new CompletionParams(docId, position);

            CompletionContext context;
            if (column > 0 && lineText.charAt(column - 1) == '.') {
                context = new CompletionContext(CompletionTriggerKind.TriggerCharacter, ".");
            } else {
                context = new CompletionContext(CompletionTriggerKind.Invoked);
            }
            params.setContext(context);

            return params;
        } catch (BadLocationException e) {
            Debug.error("Failed to create completion params: " + e.getMessage());
            return null;
        }
    }

    private List<Completion> toCompletions(Either<List<CompletionItem>, CompletionList> completionResult) {
        List<Completion> completions = new ArrayList<>();

        List<CompletionItem> items = new ArrayList<>();
        if (completionResult.isLeft()) {
            items = completionResult.getLeft();
        } else {
            items = completionResult.getRight().getItems();
        }

        for (CompletionItem item : items) {
            String label = item.getLabel();
            String insertText = item.getInsertText() != null ? item.getInsertText() : label;
            String summary = item.getDocumentation() != null
                ? item.getDocumentation().toString()
                : null;

            Completion completion = new BasicCompletion(this, insertText, label, summary);
            completions.add(completion);
        }

        return completions;
    }

    private List<Completion> mergeCompletions(List<Completion> primary, List<Completion> secondary) {
        Map<String, Completion> byInsertText = new LinkedHashMap<>();

        for (Completion completion : primary) {
            byInsertText.putIfAbsent(completion.getReplacementText(), completion);
        }

        for (Completion completion : secondary) {
            byInsertText.putIfAbsent(completion.getReplacementText(), completion);
        }

        return new ArrayList<>(byInsertText.values());
    }

    private List<Completion> filterByPrefix(List<Completion> completions, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return completions;
        }

        List<Completion> filtered = new ArrayList<>();
        String normalizedPrefix = prefix.toLowerCase();

        for (Completion completion : completions) {
            String replacement = completion.getReplacementText();
            if (replacement != null && replacement.toLowerCase().startsWith(normalizedPrefix)) {
                filtered.add(completion);
            }
        }

        return filtered;
    }

    private List<Completion> localFallbackCompletions(String prefix) {
        if (prefix == null || prefix.length() < 2) {
            return List.of();
        }

        String text = textArea.getText();
        if (text.length() > FALLBACK_SCAN_LIMIT) {
            int start = Math.max(0, textArea.getCaretPosition() - FALLBACK_SCAN_LIMIT / 2);
            int end = Math.min(text.length(), start + FALLBACK_SCAN_LIMIT);
            text = text.substring(start, end);
        }
        Matcher matcher = IDENTIFIER_PATTERN.matcher(text);
        Set<String> uniqueWords = new HashSet<>();
        List<Completion> fallback = new ArrayList<>();

        while (matcher.find()) {
            String word = matcher.group();
            if (word.equals(prefix)) {
                continue;
            }

            if (!word.startsWith(prefix)) {
                continue;
            }

            if (!uniqueWords.add(word)) {
                continue;
            }

            fallback.add(new BasicCompletion(this, word));
            if (fallback.size() >= 200) {
                break;
            }
        }

        return fallback;
    }

    @Override
    public String getAlreadyEnteredText(JTextComponent comp) {
        int offset = comp.getCaretPosition();
        try {
            int line = textArea.getLineOfOffset(offset);
            int lineStart = textArea.getLineStartOffset(line);
            int lineEnd = textArea.getLineEndOffset(line);

            String lineText = textArea.getText(lineStart, lineEnd - lineStart);
            int col = offset - lineStart;
            int start = col - 1;

            while (start >= 0 && Character.isJavaIdentifierPart(lineText.charAt(start))) {
                start--;
            }

            return lineText.substring(start + 1, col);
        } catch (BadLocationException e) {
            Debug.error("Error getting entered text: " + e.getMessage());
            return "";
        }
    }

    @Override
    public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent comp) {
        return new ArrayList<>();
    }

    @Override
    public List<Completion> getCompletionsAt(JTextComponent comp, Point p) {
        return new ArrayList<>(cachedCompletions);
    }
}
