package org.xast.xide.rc_plugin;

import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.plugin.tool.ToolOrientation;
import org.xast.xide.core.plugin.tool.ToolPlugin;
import org.xast.xide.core.plugin.ui.UIContext;
import org.xast.xide.core.utils.LucideIcon;

public class ResourceCenterPlugin implements ToolPlugin {
    @Override
    public String toolTip() {
        return "Resource Center";
    }

    @Override
    public LucideIcon icon() {
        return LucideIcon.BOXES;
    }

    @Override
    public ToolOrientation orientation() {
        return ToolOrientation.NORTH;
    }

    @Override
    public Tool tool(UIContext context) {
        return new ResourceCenterTool(
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
        return 2;
    }
}
