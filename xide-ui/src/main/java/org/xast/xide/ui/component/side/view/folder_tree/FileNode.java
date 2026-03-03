package org.xast.xide.ui.component.side.view.folder_tree;

import java.io.File;

public class FileNode {
    private final File file;
    private final boolean root;

    public FileNode(File file, boolean root) {
        this.file = file;
        this.root = root;
    }

    public File file() {
        return file;
    }

    public boolean isRoot() {
        return root;
    }

    @Override
    public String toString() {
        if (root) {
            return file.getName().toUpperCase();
        }
        return file.getName();
    }
}
