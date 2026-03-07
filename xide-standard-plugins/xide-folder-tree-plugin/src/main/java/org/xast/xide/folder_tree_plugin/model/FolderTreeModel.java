package org.xast.xide.folder_tree_plugin.model;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

public class FolderTreeModel implements TreeModel {
    private final FileNode root;
    private final List<TreeModelListener> listeners = new ArrayList<>();

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
        ensureChildrenLoaded(node);
        return node.childNodes().get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        FileNode node = (FileNode) parent;
        ensureChildrenLoaded(node);
        return node.childNodes().size();
    }

    @Override
    public boolean isLeaf(Object node) {
        return !((FileNode) node).file().isDirectory();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        FileNode p = (FileNode) parent;
        FileNode c = (FileNode) child;
        ensureChildrenLoaded(p);

        for (int i = 0; i < p.childNodes().size(); i++) {
            if (p.childNodes().get(i).file().equals(c.file())) {
                return i;
            }
        }

        return -1;
    }

    public void refreshNode(FileNode node) {
        if (!node.file().isDirectory()) {
            return;
        }

        File[] files = listSorted(node.file());
        Map<File, FileNode> existingByFile = new HashMap<>();

        for (FileNode existing : node.childNodes()) {
            existingByFile.put(existing.file(), existing);
        }

        List<FileNode> updated = new ArrayList<>(files.length);
        for (File f : files) {
            FileNode child = existingByFile.get(f);
            if (child == null) {
                child = new FileNode(f, false, node);
            }
            updated.add(child);
        }

        node.setChildNodes(updated);
        fireTreeStructureChanged(node);
    }

    public void refreshDirectory(File directory) {
        FileNode target = findNodeByFile(directory);
        if (target != null) {
            refreshNode(target);
        }
    }

    private FileNode findNodeByFile(File directory) {
        Path rootPath = root.file().toPath().toAbsolutePath().normalize();
        Path targetPath = directory.toPath().toAbsolutePath().normalize();

        if (!targetPath.startsWith(rootPath)) {
            return null;
        }

        FileNode current = root;
        if (rootPath.equals(targetPath)) {
            return current;
        }

        Path relative = rootPath.relativize(targetPath);

        for (Path segment : relative) {
            ensureChildrenLoaded(current);

            FileNode next = current.childNodes()
                .stream()
                .filter(n -> n.file().getName().equals(segment.toString()))
                .findFirst()
                .orElse(null);

            if (next == null) {
                refreshNode(current);
                next = current.childNodes()
                    .stream()
                    .filter(n -> n.file().getName().equals(segment.toString()))
                    .findFirst()
                    .orElse(null);
            }

            if (next == null) {
                return null;
            }

            current = next;
        }

        return current;
    }

    private void ensureChildrenLoaded(FileNode node) {
        if (!node.file().isDirectory()) {
            node.setChildNodes(List.of());
            return;
        }

        if (node.childrenLoaded()) {
            return;
        }

        File[] files = listSorted(node.file());
        List<FileNode> children = new ArrayList<>(files.length);

        for (File f : files) {
            children.add(new FileNode(f, false, node));
        }

        node.setChildNodes(children);
    }

    private void fireTreeStructureChanged(FileNode node) {
        TreeModelEvent event = new TreeModelEvent(this, buildPath(node));
        for (TreeModelListener listener : listeners) {
            listener.treeStructureChanged(event);
        }
    }

    private Object[] buildPath(FileNode node) {
        List<FileNode> path = new ArrayList<>();
        FileNode current = node;

        while (current != null) {
            path.add(0, current);
            current = current.parent();
        }

        return path.toArray();
    }

    public File[] listSorted(File dir) {
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
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }
}