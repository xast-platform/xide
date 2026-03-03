package org.xast.xide.ui.component.side.view.folder_tree;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.xast.xide.ui.utils.FileIconProvider;
import org.xast.xide.ui.utils.LucideIcon;

import java.awt.*;
import java.io.File;

public class FolderTreeRenderer extends DefaultTreeCellRenderer {
    private static final int ICON_SIZE = 16;
    private static final Color ICON_COLOR = new Color(200, 200, 200);

    private boolean selected;

    @Override
    public Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    ) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        this.selected = selected;

        FileNode node = (FileNode) value;
        File file = node.file();

        setText(node.toString());

        LucideIcon iconType = FileIconProvider.getIconForFile(file, ICON_SIZE);
        if (!node.isRoot()) {
            setIcon(iconType.icon(ICON_SIZE, ICON_COLOR));
        }

        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        setOpaque(false);

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (selected) {
            g2.setColor(new Color(60, 60, 60)); // selection color
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        }

        super.paintComponent(g2);
        g2.dispose();
    }
}
