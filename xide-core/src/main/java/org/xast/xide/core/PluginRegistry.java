package org.xast.xide.core;

import java.util.ArrayList;
import java.util.List;

import org.xast.xide.core.plugin.bottom.BottomPanelPlugin;
import org.xast.xide.core.plugin.tool.ToolPlugin;

import lombok.Getter;

public class PluginRegistry {
    @Getter
    private List<ToolPlugin> toolPlugins = new ArrayList<>();
    
    @Getter
    private List<BottomPanelPlugin> bottomPanelPlugins = new ArrayList<>();
    
    public void registerToolPlugin(ToolPlugin plugin) {
        toolPlugins.add(plugin);
    }
    
    public void registerBottomPanelPlugin(BottomPanelPlugin plugin) {
        bottomPanelPlugins.add(plugin);
    }
}
