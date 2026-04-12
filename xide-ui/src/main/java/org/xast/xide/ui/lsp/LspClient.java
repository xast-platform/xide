package org.xast.xide.ui.lsp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import lombok.Getter;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.*;

public class LspClient implements Closeable {
    @Getter
    private final String program;
    private final Launcher<LanguageServer> launcher;
    @Getter
    private final LanguageServer server;
    private final Process process;

    public LspClient(String program, Path workspaceRoot, LanguageClient client) 
        throws 
            IOException,
            LspServerNotFoundException 
    {        
        String pathEnv = System.getenv("PATH");
        boolean found = pathEnv != null && Stream.of(pathEnv.split(":"))
            .map(dir -> Path.of(dir, program))
            .anyMatch(p -> Files.isRegularFile(p) && Files.isExecutable(p));

        if (!found) {
            throw new LspServerNotFoundException(program);
        }

        ProcessBuilder pb = new ProcessBuilder(program);
        pb.directory(workspaceRoot.toFile());
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        this.process = pb.start();
        InputStream in = process.getInputStream();
        OutputStream out = process.getOutputStream();

        this.program = program;
        this.launcher = LSPLauncher.createClientLauncher(client, in, out);
        this.server = launcher.getRemoteProxy();
        this.launcher.startListening();

        initialize(workspaceRoot);
    }

    private void initialize(Path workspaceRoot) throws IOException {
        InitializeParams params = new InitializeParams();
        params.setProcessId(Math.toIntExact(process.pid()));
        params.setCapabilities(new ClientCapabilities());
        params.setWorkspaceFolders(
            List.of(
                new WorkspaceFolder(
                    workspaceRoot.toUri().toString(),
                    workspaceRoot.getFileName().toString()
                )
            )
        );

        try {
            server.initialize(params).get();
            server.initialized(new InitializedParams());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("LSP initialization interrupted", e);
        } catch (ExecutionException e) {
            throw new IOException("Failed to initialize LSP server", e);
        }
    }

    public void openDocument(String fileUri, String languageId, String text) {
        TextDocumentItem document = new TextDocumentItem(fileUri, languageId, 1, text);
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));
    }

    public void changeDocument(String fileUri, int version, String text) {
        VersionedTextDocumentIdentifier document = new VersionedTextDocumentIdentifier(fileUri, version);
        TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(text);
        server.getTextDocumentService().didChange(
            new DidChangeTextDocumentParams(document, List.of(change))
        );
    }

    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completionAsync(
        CompletionParams params
    ) {
        return server.getTextDocumentService().completion(params);
    }

    public void closeDocument(String fileUri) {
        server.getTextDocumentService().didClose(
            new DidCloseTextDocumentParams(new TextDocumentIdentifier(fileUri))
        );
    }

    @Override
    public void close() throws IOException {
        try {
            this.server.shutdown().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while shutting down LSP server", e);
        } catch (ExecutionException e) {
            throw new IOException("Failed to shut down LSP server", e);
        } finally {
            this.server.exit();
            this.process.destroy();
        }
    }
}
