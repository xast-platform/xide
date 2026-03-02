package org.xast.xide.ui.component.code_panel;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.xast.xide.ui.utils.XideStyle;

public class CodePanel extends JPanel {
    private JTabbedPane pane;

    public CodePanel(XideStyle style) {
        super(new GridLayout());

        pane = new JTabbedPane();
        pane.setFont(style.uiFont());
        pane.addTab("Main.xst", new EditorView(style));
        pane.addTab("Test.xst", new EditorView(style));
        add(pane);
    }
}
