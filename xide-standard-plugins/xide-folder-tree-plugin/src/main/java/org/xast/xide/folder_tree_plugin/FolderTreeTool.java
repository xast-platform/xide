package org.xast.xide.folder_tree_plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.Optional;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import org.xast.xide.core.Workspace;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.EventHandler;
import org.xast.xide.core.event.FileOpenRequestedEvent;
import org.xast.xide.core.event.FileSystemChangedEvent;
import org.xast.xide.core.event.WorkspaceChangedEvent;
import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.plugin.ui.SideBarContext;
import org.xast.xide.core.utils.Debug;
import org.xast.xide.folder_tree_plugin.components.FolderTreeView;

public class FolderTreeTool implements Tool, EventHandler {
    private Workspace workspace;
    private EventBus eventBus;
    private SideBarContext sideBar;
    private FolderTreeView view;
    private Optional<WatchService> watchService = Optional.empty();
    private Optional<Thread> watchThread = Optional.empty();
    private long lastUpdate = 0;

    public FolderTreeTool(EventBus eventBus, SideBarContext sideBar, Workspace workspace) {
        this.sideBar = sideBar;
        this.workspace = workspace;
        this.eventBus = eventBus;
        this.view = new FolderTreeView(workspace);

        setupEventListeners(eventBus);
        setupWatchService();
        startWatchThread();
    }

    public void setWorkspace(Workspace workspace) {
        stopWatchThread();

        this.workspace = workspace;
        this.view = new FolderTreeView(workspace);

        setupWatchService();
        startWatchThread();
    }

    @Override
    public void show() {
        sideBar.setView(view);
    }

    @Override
    public void setupEventListeners(EventBus eventBus) {
        view.onItemClick(file -> eventBus.publish(new FileOpenRequestedEvent(file)));

        eventBus.subscribe(WorkspaceChangedEvent.class, e -> {
            setWorkspace(e.workspace());
            show();
        });

        eventBus.subscribe(FileSystemChangedEvent.class, e -> {
            SwingUtilities.invokeLater(() -> view.refreshTree(e.dir()));
        });
    }

    private void setupWatchService() {
        if (watchService.isPresent()) {
            try {
                watchService.get().close();
                watchService = Optional.empty();
            } catch (IOException e) {
                Debug.error("Cannot close file system watch service: " + e.getMessage());
            } catch (ClosedWatchServiceException e) {
                Thread.currentThread().interrupt();
            }
        }

        Optional<File> dir = workspace.getDirectory();
        if (dir.isEmpty()) {
            watchService = Optional.empty();
            return;
        }

        try {
            watchService = Optional.of(FileSystems.getDefault().newWatchService());
            registerDirectoryTree(dir.get().toPath());
        } catch (IOException e) {
            Debug.error("Cannot handle file system watch service: " + e.getMessage());
            watchService = Optional.empty();
        }
    }

    private void registerDirectory(Path directory) throws IOException {
        if (watchService.isEmpty()) {
            return;
        }

        directory.register(
            watchService.get(),
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        );
    }

    private void registerDirectoryTree(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            stream
                .filter(Files::isDirectory)
                .forEach(path -> {
                    try {
                        registerDirectory(path);
                    } catch (IOException e) {
                        Debug.error("Cannot register directory for watching: " + path + " -> " + e.getMessage());
                    }
                });
        }
    }

    private void startWatchThread() {
        if (watchThread.isPresent() && watchThread.get().isAlive()) {
            watchThread.get().interrupt();
        }

        if (watchService.isEmpty()) return;

        watchThread = Optional.of(new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    var key = watchService.get().take();
                    Path watchedDir = (Path) key.watchable();

                    for (var event : key.pollEvents()) {
                        var kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                        Path changedRelative = (Path) event.context();
                        Path changedAbsolute = watchedDir.resolve(changedRelative).normalize();

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changedAbsolute)) {
                            try {
                                registerDirectoryTree(changedAbsolute);
                            } catch (IOException e) {
                                Debug.error("Cannot register new directory tree: " + changedAbsolute + " -> " + e.getMessage());
                            }
                        }

                        Path parentFolder = changedAbsolute.getParent() != null
                            ? changedAbsolute.getParent()
                            : watchedDir;

                        if (System.currentTimeMillis() - lastUpdate > 200) {
                            eventBus.publish(new FileSystemChangedEvent(parentFolder.toFile()));
                            lastUpdate = System.currentTimeMillis();
                        }
                    }

                    if (!key.reset()) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "FolderTree-WatchService"));

        watchThread.get().setDaemon(true);
        watchThread.get().start();
    }

    private void stopWatchThread() {
        if (watchThread.isPresent()) {
            watchThread.get().interrupt();
            watchThread = Optional.empty();
        }

        if (watchService.isPresent()) {
            try {
                watchService.get().close();
            } catch (IOException e) {
                Debug.error("Cannot close file system watch service: " + e.getMessage());
            }
            watchService = Optional.empty();
        }
    }
}
