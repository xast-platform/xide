package org.xast.xide.folder_tree_plugin.components;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.tree.TreePath;

import org.xast.xide.core.Workspace;
import org.xast.xide.core.Workspace.Directory;
import org.xast.xide.core.plugin.ui.SideBarView;
import org.xast.xide.folder_tree_plugin.model.FileNode;
import org.xast.xide.folder_tree_plugin.model.FolderTreeModel;
import org.xast.xide.ui.components.CenteredLabel;

public class FolderTreeView extends SideBarView {
    private Optional<JTree> tree = Optional.empty();

    public FolderTreeView(Workspace workspace) {
        setLayout(new GridLayout());

        JComponent currentView;
        Optional<File> fileNode = Optional.empty();

        switch (workspace) {
            case Workspace.Directory directory -> {
                fileNode = Optional.of(directory.dir());
            }
            case Workspace.Combined combined -> {
                fileNode = Arrays
                    .stream(combined.workspaces())
                    .filter(ws -> ws instanceof Directory)
                    .map(ws -> ((Directory) ws).dir())
                    .findAny();
            }
            default -> {}
        }

        if (fileNode.isPresent()) {
            currentView = new FolderTree(new FolderTreeModel(fileNode.get()));
        } else {
            currentView = new CenteredLabel("No workspace opened", 100, 18f);
        }

        JScrollPane scrollPane = new JScrollPane(currentView);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        add(scrollPane);
    }

    public void onItemClick(Consumer<File> consumer) {
        if (tree.isEmpty()) {
            return;
        }

        tree.get().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.get().getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;

                    FileNode node = (FileNode) path.getLastPathComponent();
                    File file = node.file();

                    if (file.isFile()) {
                        consumer.accept(file);
                    }
                }
            }
        });
    }
}
