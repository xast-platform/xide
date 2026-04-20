package org.xast.xide.search_plugin;

import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.plugin.tool.ToolOrientation;
import org.xast.xide.core.plugin.tool.ToolPlugin;
import org.xast.xide.core.plugin.ui.UIContext;
import org.xast.xide.core.utils.LucideIcon;

public class SearchPlugin implements ToolPlugin {
    @Override
    public String toolTip() {
        return "Search..";
    }

    @Override
    public LucideIcon icon() {
        return LucideIcon.SEARCH;
    }

    @Override
    public ToolOrientation orientation() {
        return ToolOrientation.NORTH;
    }

    @Override
    public Tool tool(UIContext context) {
        return new SearchTool(
            context.eventBus(),
            context.sideBar(), 
            context.currentWorkspace()
        );
    }

    @Override
    public boolean runAtStartup() {
        return false;
    }

    @Override
    public int priority() {
        return 1;
    }
}
