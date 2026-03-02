package org.xast.xide.ui.component.code_panel;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.xast.xide.ui.utils.XideStyle;

public class CodePanel extends JPanel {
    private JTabbedPane pane;

    public CodePanel() {
        super(new GridLayout());

        XideStyle style = XideStyle.getCurrent();

        pane = new JTabbedPane();
        pane.setFont(style.uiFont());
        pane.addTab("Main.xst", new EditorView());
        pane.addTab("Test.xst", new EditorView());
        add(pane);
    }
}
