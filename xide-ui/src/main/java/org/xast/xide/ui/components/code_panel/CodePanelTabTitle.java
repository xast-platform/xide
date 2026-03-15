package org.xast.xide.ui.components.code_panel;

import javax.swing.JLabel;

public class CodePanelTabTitle extends JLabel {
    private final CodePanelTabModel model;

    public CodePanelTabTitle(CodePanelTabModel model) {
        super();
        this.model = model;
        refresh();
    }

    public void refresh() {
        String title = model.getFile().getName();
        if (!model.isSaved()) {
            title += "*";
        }
        setText(title);
    }
}
