package org.xast.xide.ui.components.side.view.folder_tree;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;

import org.xast.xide.core.Workspace;
import org.xast.xide.core.Workspace.Directory;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.FileOpenRequestedEvent;
import org.xast.xide.ui.components.side.view.SideBarView;
import org.xast.xide.ui.utils.XideStyle;

public class FolderTreeView extends SideBarView {
    public FolderTreeView(Workspace workspace) {
        XideStyle style = XideStyle.getCurrent();

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
            JTree tree = new JTree(new FolderTreeModel(fileNode.get()));
            tree.setRootVisible(true);
            tree.setFont(style.uiFont().deriveFont(17f));
            tree.setCellRenderer(new FolderTreeRenderer());
            tree.setRowHeight(28);
            tree.setOpaque(false);
            tree.setBackground(new Color(0,0,0,0));
            tree.setUI(new BasicTreeUI() {
                @Override
                protected void paintRow(
                    Graphics g, 
                    Rectangle clipBounds, 
                    Insets insets, 
                    Rectangle bounds, 
                    TreePath path, 
                    int row, 
                    boolean isExpanded, 
                    boolean hasBeenExpanded, 
                    boolean leaf
                ) {
                    super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, leaf);
                }
            });
            tree.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                        if (path == null) return;

                        FileNode node = (FileNode) path.getLastPathComponent();
                        File file = node.file();

                        if (file.isFile()) {
                            EventBus.getInstance().publish(new FileOpenRequestedEvent(file));
                        }
                    }
                }
            });

            currentView = tree;
        } else {
            JLabel label = new JLabel(
                "<html><div style='text-align: center; width: 100px;'>No workspace opened</div></html>"
            );
            label.setFont(style.uiFont().deriveFont(18f));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);

            currentView = label;
        }

        JScrollPane scrollPane = new JScrollPane(currentView);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        add(scrollPane);
    }
}
