package org.xast.xide.ui.lsp;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.xast.xide.core.utils.Debug;

public class LspServerRegistry implements Closeable {
    private final Map<String, LspServerDefinition> serversById;
    private final Map<String, String> serverIdByExtension;
    private final Map<String, Boolean> enabledById;
    private final Map<String, LspClient> clientsById;
    private Path workspaceRoot;

    public LspServerRegistry(Optional<Path> workspaceRoot) {
        this.serversById = new HashMap<>();
        this.serverIdByExtension = new HashMap<>();
        this.enabledById = new HashMap<>();
        this.clientsById = new HashMap<>();
        this.workspaceRoot = workspaceRoot.orElse(defaultWorkspaceRoot());

        register(
            new LspServerDefinition(
                "rust-analyzer",
                "rust-analyzer",
                "rust",
                Set.of("rs")
            )
        );
    }

    public void register(LspServerDefinition definition) {
        serversById.put(definition.id(), definition);
        enabledById.putIfAbsent(definition.id(), true);

        for (String extension : definition.fileExtensions()) {
            serverIdByExtension.put(extension, definition.id());
        }
    }

    public boolean isEnabled(String serverId) {
        return enabledById.getOrDefault(serverId, false);
    }

    public void setEnabled(String serverId, boolean enabled) {
        enabledById.put(serverId, enabled);
        if (!enabled) {
            closeClient(serverId);
        }
    }

    public Optional<LspClient> clientFor(File file) {
        String serverId = serverIdByExtension.get(fileExtension(file));
        if (serverId == null || !isEnabled(serverId)) {
            return Optional.empty();
        }

        LspClient client = clientsById.get(serverId);
        if (client != null) {
            return Optional.of(client);
        }

        LspServerDefinition definition = serversById.get(serverId);
        if (definition == null) {
            return Optional.empty();
        }

        try {
            client = new LspClient(
                definition.command(),
                workspaceRoot,
                new XideLanguageClient()
            );
            clientsById.put(serverId, client);
            return Optional.of(client);
        } catch (IOException | LspServerNotFoundException e) {
            Debug.error("Cannot start " + definition.id() + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> languageIdFor(File file) {
        String serverId = serverIdByExtension.get(fileExtension(file));
        if (serverId == null) {
            return Optional.empty();
        }

        LspServerDefinition definition = serversById.get(serverId);
        return definition == null ? Optional.empty() : Optional.of(definition.languageId());
    }

    public void updateWorkspaceRoot(Optional<Path> workspaceRoot) {
        Path newWorkspaceRoot = workspaceRoot.orElse(defaultWorkspaceRoot());
        if (Objects.equals(this.workspaceRoot, newWorkspaceRoot)) {
            return;
        }

        closeAllClients();
        this.workspaceRoot = newWorkspaceRoot;
    }

    private void closeAllClients() {
        for (String serverId : List.copyOf(clientsById.keySet())) {
            closeClient(serverId);
        }
    }

    private void closeClient(String serverId) {
        LspClient client = clientsById.remove(serverId);
        if (client == null) {
            return;
        }

        try {
            client.close();
        } catch (IOException e) {
            Debug.error("Failed to stop " + serverId + ": " + e.getMessage());
        }
    }

    private Path defaultWorkspaceRoot() {
        return Path.of(System.getProperty("user.dir"));
    }

    private String fileExtension(File file) {
        String fileName = file.getName();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex == -1 || extensionIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionIndex + 1);
    }

    @Override
    public void close() {
        closeAllClients();
    }
}