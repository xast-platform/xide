package org.xast.xide.ui.lsp;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.xast.xide.core.utils.Debug;

public class XideLanguageClient implements LanguageClient {
    @Override
    public void telemetryEvent(Object object) {}

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
        // Avoid logging full diagnostic payloads on every keystroke.
    }

    @Override
    public void showMessage(MessageParams messageParams) {
        Debug.info("LSP message: " + messageParams.getMessage());
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
        return CompletableFuture.completedFuture(
            new MessageActionItem("OK")
        );
    }

    @Override
    public void logMessage(MessageParams message) {
        Debug.info("LSP log: " + message.getMessage());
    }
}
