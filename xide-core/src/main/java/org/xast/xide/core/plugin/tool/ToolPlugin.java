package org.xast.xide.core.plugin.tool;

import org.xast.xide.core.plugin.ui.UIContext;
import org.xast.xide.core.utils.LucideIcon;

public interface ToolPlugin {
    String toolTip();

    ToolOrientation orientation();

    LucideIcon icon();

    Tool tool(UIContext context);

    boolean runAtStartup();

    int priority();
}
