package org.xast.xide.core;

import java.util.ServiceLoader;

import org.xast.xide.core.plugin.bottom.BottomPanelPlugin;
import org.xast.xide.core.plugin.file.FilePlugin;
import org.xast.xide.core.plugin.tool.ToolPlugin;

import lombok.Getter;

public class PluginManager {
    @Getter
    private PluginRegistry registry;

    public PluginManager() {
        registry = new PluginRegistry();
    }

    public void loadPlugins() {
        ServiceLoader<ToolPlugin> toolPlugins = ServiceLoader.load(ToolPlugin.class);
        for (ToolPlugin plugin : toolPlugins) {
            registry.registerToolPlugin(plugin);
        }
        
        ServiceLoader<BottomPanelPlugin> bottomPlugins = ServiceLoader.load(BottomPanelPlugin.class);
        for (BottomPanelPlugin plugin : bottomPlugins) {
            registry.registerBottomPanelPlugin(plugin);
        }

        ServiceLoader<FilePlugin> filePlugins = ServiceLoader.load(FilePlugin.class);
        for (FilePlugin plugin : filePlugins) {
            registry.registerFilePlugin(plugin);
        }
    }
}
