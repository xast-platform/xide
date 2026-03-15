package org.xast.xide.core.plugin.ui;

import java.awt.GridLayout;

import javax.swing.JPanel;

import org.xast.xide.core.plugin.file.FileModel;

public abstract class CodePanelView extends JPanel {
    public CodePanelView() {
        super(new GridLayout());
    }

    public abstract FileModel model();
}
