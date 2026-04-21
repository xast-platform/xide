package org.xast.xide.core.plugin.ui;

import javax.swing.JFrame;

import org.xast.xide.core.Workspace;
import org.xast.xide.core.config.XideConfig;
import org.xast.xide.core.event.EventBus;

public interface UIContext {
    Workspace currentWorkspace();

    SideBarContext sideBar();

    JFrame frame();

    EventBus eventBus();

    XideConfig config();
}
