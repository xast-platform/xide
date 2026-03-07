package org.xast.xide.folder_tree_plugin.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileNode {
    private final File file;
    private final boolean root;
    private final FileNode parent;
    private List<FileNode> childNodes = new ArrayList<>();
    private boolean childrenLoaded;

    public FileNode(File file, boolean root) {
        this(file, root, null);
    }

    public FileNode(File file, boolean root, FileNode parent) {
        this.file = file;
        this.root = root;
        this.parent = parent;
    }

    public File file() {
        return file;
    }

    public boolean isRoot() {
        return root;
    }

    public FileNode parent() {
        return parent;
    }

    public List<FileNode> childNodes() {
        return childNodes;
    }

    public void setChildNodes(List<FileNode> childNodes) {
        this.childNodes = childNodes;
        this.childrenLoaded = true;
    }

    public boolean childrenLoaded() {
        return childrenLoaded;
    }

    @Override
    public String toString() {
        if (root) {
            return "<html><b>" + file.getName().toUpperCase() + "</b></html>";
        }
        return file.getName();
    }
}
