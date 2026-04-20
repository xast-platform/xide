package org.xast.xide.search_plugin;

import org.xast.xide.core.Workspace;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.EventHandler;
import org.xast.xide.core.event.WorkspaceChangedEvent;
import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.plugin.ui.SideBarContext;

public class SearchTool implements Tool, EventHandler {
    private Workspace workspace;
    private EventBus eventBus;
    private SideBarContext sideBar;
    private SearchView view;

    public SearchTool(EventBus eventBus, SideBarContext sideBar, Workspace workspace) {
        this.sideBar = sideBar;
        this.workspace = workspace;
        this.eventBus = eventBus;
        this.view = new SearchView(workspace);

        setupEventListeners(eventBus);
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
        this.view = new SearchView(workspace);
    }

    @Override
    public void show() {
        sideBar.setView(view);
    }

    @Override
    public void setupEventListeners(EventBus eventBus) {
        eventBus.subscribe(WorkspaceChangedEvent.class, e -> {
            setWorkspace(e.workspace());
            show();
        });
    }
}
