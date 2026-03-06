package org.xast.xide.ui.components.code_panel;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CodePanelTabTitle extends JLabel {
    private boolean saved;
    private JTabbedPane pane;
    private CodePanelTab tab;

    @Override
    public String getText() {
        int index = pane.indexOfTabComponent(tab);
        if (index != -1) {
            String title = pane.getTitleAt(index);
            if (!saved) {
                title += "*";
            }
            return title;
        }
        return "";
    }
}
