package org.xast.xide.folder_tree_plugin.components;

import java.awt.*;

import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;

import org.xast.xide.folder_tree_plugin.model.FolderTreeModel;
import org.xast.xide.ui.utils.XideStyle;

public class FolderTree extends JTree {
    public FolderTree(FolderTreeModel model) {
        super(model);

        XideStyle style = XideStyle.getCurrent();

        setRootVisible(true);
        setFont(style.uiFont().deriveFont(17f));
        setCellRenderer(new FolderTreeRenderer());
        setRowHeight(28);
        setOpaque(false);
        setBackground(new Color(0,0,0,0));
        setUI(new BasicTreeUI() {
            @Override
            protected void paintRow(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path, int row, boolean isExpanded, boolean hasBeenExpanded, boolean leaf) {
                super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, leaf);
            }
        });
    }
}
