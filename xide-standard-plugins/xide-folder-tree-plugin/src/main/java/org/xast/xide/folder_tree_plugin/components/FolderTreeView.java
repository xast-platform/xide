package org.xast.xide.folder_tree_plugin.components;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import org.xast.xide.core.Workspace;
import org.xast.xide.core.plugin.ui.SideBarView;
import org.xast.xide.folder_tree_plugin.model.FileNode;
import org.xast.xide.folder_tree_plugin.model.FolderTreeModel;
import org.xast.xide.ui.components.CenteredLabel;

public class FolderTreeView extends SideBarView {
    private Optional<FolderTreeModel> model = Optional.empty();
    private Optional<FolderTree> tree = Optional.empty();

    public FolderTreeView(Workspace workspace) {
        setLayout(new GridLayout());

        JComponent currentView;
        Optional<File> fileNode = workspace.getDirectory();     

        if (fileNode.isPresent()) {
            model = Optional.of(new FolderTreeModel(fileNode.get()));
            tree = Optional.of(new FolderTree(model.get()));
            currentView = tree.get();
        } else {
            currentView = new CenteredLabel("No workspace opened", 100, 18f);
        }

        JScrollPane scrollPane = new JScrollPane(currentView);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(scrollPane);
    }

    public void refreshTree(File directory) {
        if (tree.isEmpty() || model.isEmpty() || directory == null) {
            return;
        }
        model.get().refreshDirectory(directory);
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

    public void onDirectoryExpanded(Consumer<File> consumer) {
        if (tree.isEmpty()) {
            return;
        }

        tree.get().addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                if (path == null) {
                    return;
                }

                FileNode node = (FileNode) path.getLastPathComponent();
                File file = node.file();

                if (file.isDirectory()) {
                    consumer.accept(file);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {}
        });
    }
}
