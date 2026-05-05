package org.xast.xide.settings_plugin

import org.xast.xide.core.plugin.tool.Tool
import org.xast.xide.core.plugin.tool.ToolOrientation
import org.xast.xide.core.plugin.tool.ToolPlugin
import org.xast.xide.core.plugin.ui.UIContext
import org.xast.xide.core.utils.LucideIcon

class SettingsPlugin : ToolPlugin {
    override fun toolTip(): String = "Settings"

    override fun orientation(): ToolOrientation = ToolOrientation.SOUTH

    override fun icon(): LucideIcon = LucideIcon.COG

    override fun tool(context: UIContext): Tool = SettingsTool(context.frame())
        
    override fun runAtStartup(): Boolean = false

    override fun priority(): Int = 0
}
