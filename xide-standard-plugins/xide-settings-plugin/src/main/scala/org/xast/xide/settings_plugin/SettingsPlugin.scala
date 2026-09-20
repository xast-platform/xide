package org.xast.xide.settings_plugin

import org.xast.xide.core.plugin.tool.Tool
import org.xast.xide.core.plugin.tool.ToolOrientation
import org.xast.xide.core.plugin.tool.ToolPlugin
import org.xast.xide.core.plugin.ui.UIContext
import org.xast.xide.core.utils.LucideIcon

class SettingsPlugin extends ToolPlugin {
    override def toolTip(): String = "Settings"

    override def orientation(): ToolOrientation = ToolOrientation.SOUTH

    override def icon(): LucideIcon = LucideIcon.COG

    override def tool(context: UIContext): Tool = SettingsTool(context.frame())
        
    override def runAtStartup(): Boolean = false

    override def priority(): Int = 0
}
