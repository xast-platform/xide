package org.xast.xide.core;

import lombok.Getter;

public class PluginManager {
    @Getter
    private PluginRegistry registry;

    public PluginManager() {
        registry = new PluginRegistry();
    }

    public void loadPlugins() {
        
    }
}
