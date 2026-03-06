package org.xast.xide.ui.components.code_panel;

import java.awt.GridLayout;
import java.io.File;
import java.util.HashSet;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.FileOpenRequestedEvent;
import org.xast.xide.core.event.TabCloseRequestedEvent;
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
        
        setupEventListeners();
    }
    
    private void setupEventListeners() {
        EventBus eventBus = EventBus.getInstance();
        
        eventBus.subscribe(FileOpenRequestedEvent.class, event -> {
            openFile(event.file());
        });
        
        eventBus.subscribe(TabCloseRequestedEvent.class, event -> {
            closeFile(event.file());
        });
    }
    
    private void closeFile(File file) {
        if (!openedFiles.contains(file)) {
            return;
        }
        
        openedFiles.remove(file);
        
        for (int i = 0; i < pane.getTabCount(); i++) {
            var component = pane.getTabComponentAt(i);
            if (component instanceof CodePanelTab) {
                String tabTitle = pane.getTitleAt(i);
                if (tabTitle.equals(file.getName()) || tabTitle.equals(file.getName() + "*")) {
                    pane.remove(i);
                    break;
                }
            }
        }
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
