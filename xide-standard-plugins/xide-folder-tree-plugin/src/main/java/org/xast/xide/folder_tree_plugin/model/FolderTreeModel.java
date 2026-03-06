package org.xast.xide.folder_tree_plugin.model;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FolderTreeModel implements TreeModel {
    private final FileNode root;

    public FolderTreeModel(File rootDir) {
        this.root = new FileNode(rootDir, true);
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        FileNode node = (FileNode) parent;
        File[] children = listSorted(node.file());
        
        return new FileNode(children[index], false);
    }

    @Override
    public int getChildCount(Object parent) {
        FileNode node = (FileNode) parent;
        File[] children = listSorted(node.file());

        return children == null ? 0 : children.length;
    }

    @Override
    public boolean isLeaf(Object node) {
        return !((FileNode) node).file().isDirectory();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        FileNode p = (FileNode) parent;
        FileNode c = (FileNode) child;
        File[] children = listSorted(p.file());

        return Arrays
            .asList(children)
            .indexOf(c.file());
    }

    private File[] listSorted(File dir) {
        File[] children = dir.listFiles();
        if (children == null) return new File[0];

        Arrays.sort(
            children, 
            Comparator
                .comparing(File::isFile)
                .thenComparing(f -> f.getName().toLowerCase())
        );

        return children;
    }

    @Override 
    public void valueForPathChanged(TreePath path, Object newValue) {}
    @Override 
    public void addTreeModelListener(TreeModelListener l) {}
    @Override 
    public void removeTreeModelListener(TreeModelListener l) {}
}