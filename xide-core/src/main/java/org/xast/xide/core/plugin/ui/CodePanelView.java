package org.xast.xide.core.plugin.ui;

import java.awt.GridLayout;

import javax.swing.JPanel;

import org.xast.xide.core.plugin.file.FileModel;

public abstract class CodePanelView extends JPanel implements AutoCloseable {
    public CodePanelView() {
        super(new GridLayout());
    }

    public abstract FileModel model();

    @Override
    public void close() throws Exception {
    }
}
