package org.xast.xide.terminal_plugin;

import org.xast.xide.core.plugin.bottom.BottomPanelPlugin;
import org.xast.xide.core.plugin.bottom.BottomPanelView;

public class TerminalPlugin implements BottomPanelPlugin {
    @Override
    public String tabName() {
        return "Terminal";
    }

    @Override
    public BottomPanelView view() {
        return new TerminalView();
    }
}
