package org.xast.xide.folder_tree_plugin.components;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;

import org.xast.xide.folder_tree_plugin.model.FolderTreeModel;
import org.xast.xide.ui.utils.XideStyle;

public class FolderTree extends JTree {
    private int hoveredRow = -1;

    public FolderTree(FolderTreeModel model) {
        super(model);

        XideStyle style = XideStyle.getCurrent();

        setRootVisible(true);
        setFont(style.uiFont());
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
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = getRowForLocation(e.getX(), e.getY());

                if (row != hoveredRow) {
                    hoveredRow = row;
                    repaint();
                }
            }
        });
    }

    public int getHoveredRow() {
        return hoveredRow;
    }
}
