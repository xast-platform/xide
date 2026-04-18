package org.xast.xide.ui.components.code_panel;

import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.xast.xide.core.PluginRegistry;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.EventHandler;
import org.xast.xide.core.event.FileOpenRequestedEvent;
import org.xast.xide.core.event.FileSaveRequestedEvent;
import org.xast.xide.core.event.TabCloseRequestedEvent;
import org.xast.xide.core.plugin.file.FilePlugin;
import org.xast.xide.core.plugin.ui.CodePanelView;
import org.xast.xide.core.utils.Debug;
import org.xast.xide.ui.utils.SyntaxStyle;
import org.xast.xide.ui.utils.XideStyle;

public class CodePanel extends JPanel implements EventHandler {
    private JTabbedPane pane;
    private HashSet<File> openedFiles;
    private EventBus eventBus;
    private PluginRegistry pluginRegistry;

    public CodePanel(EventBus eventBus, PluginRegistry pluginRegistry) {
        super(new GridLayout());

        XideStyle style = XideStyle.getCurrent();

        this.eventBus = eventBus;
        this.pane = new JTabbedPane();
        this.pane.setFont(style.uiFont());
        this.openedFiles = new HashSet<>();
        this.pluginRegistry = pluginRegistry;
        
        add(pane);   
        
        setupEventListeners(eventBus);
    }
    
    @Override
    public void setupEventListeners(EventBus eventBus) {        
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

    private void openFile(File file) {
        if (file.isDirectory()) {
            return;
        }

        if (!openedFiles.add(file)) {
            return;
        }

        String ext = file.getName().contains(".") 
            ? file.getName().substring(file.getName().lastIndexOf(".") + 1) 
            : "";

        Optional<FilePlugin> plugin = Optional.ofNullable(
            pluginRegistry.getFilePlugins().get(ext)
        );

        pane.addTab(
            file.getName(), 
            plugin.isPresent()
                ? plugin.get().view(eventBus, file)
                : new EditorView(eventBus, file, SyntaxStyle.Plain, 4)
        );

        int index = pane.getTabCount() - 1;
        CodePanelTabModel model = new CodePanelTabModel(file.exists(), file);
        pane.setTabComponentAt(index, new CodePanelTab(eventBus, pane, model));
        pane.setSelectedIndex(index);
    }

    public void saveCurrentFile() {
        int index = pane.getSelectedIndex();
        if (index == -1) {
            return;
        }

        var component = pane.getTabComponentAt(index);
        if (component instanceof CodePanelTab) {
            CodePanelTab tab = (CodePanelTab) component;
            File file = tab.getModel().getFile();

            Optional<CodePanelView> view = currentView();

            if (view.isEmpty()) {
                return;
            }

            try {
                view.get().model().saveToFile(file);
            } catch (IOException e) {
                Debug.error("Cannot save file `" + file.getName() + "`: " + e.getMessage());
                return;
            }

            eventBus.publish(new FileSaveRequestedEvent(file, true));
        }
    }

    public Optional<CodePanelView> currentView() {
        int index = pane.getSelectedIndex();
        if (index == -1) {
            return Optional.empty();
        }

        var component = pane.getComponentAt(index);
        if (component instanceof CodePanelView) {
            return Optional.of((CodePanelView) component);
        }

        return Optional.empty();
    }
}
