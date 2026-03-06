package org.xast.xide.folder_tree_plugin;

import org.xast.xide.core.Workspace;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.EventHandler;
import org.xast.xide.core.event.FileOpenRequestedEvent;
import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.plugin.ui.SideBarContext;
import org.xast.xide.core.plugin.ui.SideBarView;
import org.xast.xide.folder_tree_plugin.components.FolderTreeView;

public class FolderTreeTool implements Tool, EventHandler {
    private SideBarContext sideBar;
    private SideBarView view;

    public FolderTreeTool(EventBus eventBus, SideBarContext sideBar, Workspace workspace) {
        this.sideBar = sideBar;
        this.view = new FolderTreeView(workspace);  
        
        setupEventListeners(eventBus);
    }

    public void setWorkspace(Workspace workspace) {
        this.view = new FolderTreeView(workspace);
    }

    @Override
    public void show() {
        sideBar.setView(view);
    }

    @Override
    public void setupEventListeners(EventBus eventBus) {
        ((FolderTreeView) view).onItemClick(file -> {
            eventBus.publish(new FileOpenRequestedEvent(file));
        });
    }
}
