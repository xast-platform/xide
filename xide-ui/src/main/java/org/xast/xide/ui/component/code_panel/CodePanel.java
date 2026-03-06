package org.xast.xide.ui.component.code_panel;

import java.awt.GridLayout;
import java.io.File;
import java.util.HashSet;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.xast.xide.ui.utils.XideStyle;

public class CodePanel extends JPanel {
    private JTabbedPane pane;
    private HashSet<File> openedFiles;

    public CodePanel() {
        super(new GridLayout());

        XideStyle style = XideStyle.getCurrent();

        pane = new JTabbedPane();
        pane.setFont(style.uiFont());
        openedFiles = new HashSet<>();
        
        add(pane);
    }

    public void openFile(File file) {
        if (file.isDirectory()) {
            return;
        }

        if (!openedFiles.add(file)) {
            return;
        }

        pane.addTab(file.getName(), new EditorView(file));

        int index = pane.getTabCount() - 1;
        pane.setTabComponentAt(index, new CodePanelTab(pane, openedFiles, file, file.exists()));
    }
}
