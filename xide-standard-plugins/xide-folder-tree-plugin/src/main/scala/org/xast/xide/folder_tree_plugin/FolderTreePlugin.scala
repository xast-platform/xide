package org.xast.xide.folder_tree_plugin

import org.xast.xide.core.plugin.tool.Tool
import org.xast.xide.core.plugin.tool.ToolOrientation
import org.xast.xide.core.plugin.tool.ToolPlugin
import org.xast.xide.core.plugin.ui.UIContext
import org.xast.xide.core.utils.LucideIcon

class FolderTreePlugin extends ToolPlugin:
    
    override def toolTip: String = "Project tree"

    override def icon: LucideIcon = LucideIcon.FOLDER_TREE

    override def orientation: ToolOrientation = ToolOrientation.NORTH

    override def tool(context: UIContext): Tool = new FolderTreeTool(
        context.eventBus,
        context.sideBar, 
        context.currentWorkspace,
    )

    override def runAtStartup: Boolean = true
    
    override def priority: Int = 0