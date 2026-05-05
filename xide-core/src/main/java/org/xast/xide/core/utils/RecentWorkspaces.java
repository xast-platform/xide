package org.xast.xide.core.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.xast.xide.core.Workspace;
import org.xast.xide.core.config.XideConfig;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.EventHandler;
import org.xast.xide.core.event.WorkspaceChangedEvent;

import lombok.Getter;

public class RecentWorkspaces implements EventHandler {
    public static final String RECENT_FILE = "recent.txt";

    @Getter
    private HashSet<Workspace> workspaces;

    public RecentWorkspaces(EventBus eventBus) {
        this.workspaces = new HashSet<>();
        Path path = getConfigPath();

        loadWorkspaces(path);
        saveWorkspaces();
        setupEventListeners(eventBus);
    }

    @Override
    public void setupEventListeners(EventBus eventBus) {
        eventBus.subscribe(WorkspaceChangedEvent.class, e -> {
            addWorkspace(e.workspace());
        });
    }

    public void clearWorkspaces() {
        workspaces.clear();
        saveWorkspaces();
    }

    public void addWorkspace(Workspace workspace) {
        if (workspace.getPath().isEmpty()) {
            return;
        }

        workspaces.remove(workspace);
        workspaces.add(workspace);

        saveWorkspaces();
    }

    public void saveWorkspaces() {
        Path path = getConfigPath();

        try {
            Files.createDirectories(path.getParent());
            Files.write(
                path, 
                workspaces.stream()
                    .map(Workspace::getPath)
                    .flatMap(Optional::stream)
                    .map(Path::toString)
                    .toList()
            );
        } catch (IOException e) {
            Debug.error("Cannot save recent workspaces to `"+RECENT_FILE+"`: "+e);
        }    
    }

    private void loadWorkspaces(Path path) {
        File file = path.toFile();

        if (!file.exists()) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                Debug.error("Cannot create `"+RECENT_FILE+"`: "+e);
                return;
            }
        }

        if (!file.isFile()) {
            Debug.error("`"+RECENT_FILE+"` is not a file");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }

                File wsFile = new File(line.trim());
                if (!wsFile.exists()) {
                    continue;
                }

                if (wsFile.isFile()) {
                    workspaces.add(new Workspace.ExistingFile(wsFile));
                } else if (wsFile.isDirectory()) {
                    workspaces.add(new Workspace.Directory(wsFile));
                }
            }
        } catch (IOException e) {
            Debug.error("Cannot read `"+RECENT_FILE+"`: "+e);
        }
    }

    private Path getConfigPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData != null ? appData : home, XideConfig.CONFIG_FOLDER, RECENT_FILE);
        }

        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", XideConfig.CONFIG_FOLDER, RECENT_FILE);
        }

        // Linux / BSD / others → XDG
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, XideConfig.CONFIG_FOLDER, RECENT_FILE);
        }

        return Path.of(home, ".config", XideConfig.CONFIG_FOLDER, RECENT_FILE);
    }
}
