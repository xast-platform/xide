package org.xast.xide.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xast.xide.core.plugin.bottom.BottomPanelPlugin;
import org.xast.xide.core.plugin.file.FilePlugin;
import org.xast.xide.core.plugin.tool.ToolPlugin;
import org.xast.xide.core.utils.Debug;

import lombok.Getter;

public class PluginRegistry {
    @Getter
    private Map<String, FilePlugin> filePlugins = new HashMap<>();
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

    public void registerFilePlugin(FilePlugin plugin) {
        String ext = plugin.fileExtension();
        if (filePlugins.containsKey(ext)) {
            Debug.error("File plugin for extension `" + ext + "` already registered");
            return;
        }

        filePlugins.put(ext, plugin);
    }
}
