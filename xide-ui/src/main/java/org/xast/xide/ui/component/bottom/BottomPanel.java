package org.xast.xide.ui.component.bottom;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;

import org.xast.xide.ui.utils.XideStyle;

public class BottomPanel extends JPanel {
    private JTabbedPane pane;

    public BottomPanel() {
        setLayout(new BorderLayout());

        XideStyle style = XideStyle.getCurrent();

        pane = new JTabbedPane();
        pane.setFont(style.uiFont());
        pane.addTab("Terminal", new TerminalView());

        add(pane, BorderLayout.CENTER);
    }
}
