package org.xast.xide.folder_tree_plugin;

import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.plugin.tool.ToolOrientation;
import org.xast.xide.core.plugin.tool.ToolPlugin;
import org.xast.xide.core.plugin.ui.UIContext;
import org.xast.xide.core.utils.LucideIcon;

public class FolderTreePlugin implements ToolPlugin {
    @Override
    public String toolTip() {
        return "Project tree";
    }

    @Override
    public LucideIcon icon() {
        return LucideIcon.FOLDER_TREE;
    }

    @Override
    public ToolOrientation orientation() {
        return ToolOrientation.NORTH;
    }

    @Override
    public Tool tool(UIContext context) {
        return new FolderTreeTool(
            context.eventBus(),
            context.sideBar(), 
            context.currentWorkspace()
        );
    }
}
