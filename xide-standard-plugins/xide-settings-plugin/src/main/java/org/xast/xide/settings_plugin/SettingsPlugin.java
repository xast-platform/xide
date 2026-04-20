package org.xast.xide.settings_plugin;

import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.plugin.tool.ToolOrientation;
import org.xast.xide.core.plugin.tool.ToolPlugin;
import org.xast.xide.core.plugin.ui.UIContext;
import org.xast.xide.core.utils.LucideIcon;

public class SettingsPlugin implements ToolPlugin {
    @Override
    public String toolTip() {
        return "Settings";
    }

    @Override
    public ToolOrientation orientation() {
        return ToolOrientation.SOUTH;
    }

    @Override
    public LucideIcon icon() {
        return LucideIcon.COG;
    }

    @Override
    public Tool tool(UIContext context) {
        return new SettingsTool(context.frame());
    }
    
    @Override
    public boolean runAtStartup() {
        return false;
    }

    @Override
    public int priority() {
        return 0;
    }
}
