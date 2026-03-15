package org.xast.xide.ui.components.code_panel;

import javax.swing.JLabel;

import org.xast.xide.core.utils.Debug;

public class CodePanelTabTitle extends JLabel {
    private final CodePanelTabModel model;

    public CodePanelTabTitle(CodePanelTabModel model) {
        super();
        this.model = model;
        refresh();
    }

    public void refresh() {
        Debug.info("Refreshing tab title for file: " + model.getFile().getName() + ", saved: " + model.isSaved());
        String title = model.getFile().getName();
        if (!model.isSaved()) {
            title += "*";
        }
        setText(title);
    }
}
