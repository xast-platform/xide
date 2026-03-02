package org.xast.xide.ui.component.side.view;

import java.awt.GridLayout;
import java.io.File;
import java.util.Optional;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.xast.xide.core.Workspace;
import org.xast.xide.ui.utils.XideStyle;

public class FolderTreeView extends SideBarView {
    public FolderTreeView(Workspace workspace) {
        XideStyle style = XideStyle.getCurrent();

        setLayout(new GridLayout());

        JTree tree = new JTree(createWorkspaceNode(workspace));
        tree.setRootVisible(true);
        tree.setFont(style.uiFont().deriveFont(16));

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(scrollPane);
    }

    private DefaultMutableTreeNode createWorkspaceNode(Workspace workspace) {
        return switch (workspace) {
            case Workspace.Directory directory -> createDirectoryNode(directory.dir());
            case Workspace.Combined combined -> {
                DefaultMutableTreeNode root = new DefaultMutableTreeNode("Workspace");
                for (Workspace childWorkspace : combined.workspaces()) {
                    var node = createWorkspaceNode(childWorkspace);
                    if (!node.getUserObject().equals("Empty workspace")) {
                        root.add(node);
                    }
                }

                yield root;
            }
            default -> new DefaultMutableTreeNode("Empty workspace");
        };
    }

    private DefaultMutableTreeNode createDirectoryNode(File directory) {
        DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(directory.getName());
        File[] children = directory.listFiles();

        if (children == null) {
            return dirNode;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                dirNode.add(createDirectoryNode(child));
            } else {
                dirNode.add(new DefaultMutableTreeNode(child.getName()));
            }
        }

        return dirNode;
    }
}
