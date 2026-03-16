package org.xast.xide.logs_plugin;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.plugin.bottom.BottomPanelPlugin;
import org.xast.xide.core.plugin.bottom.BottomPanelView;

public class LogsPlugin implements BottomPanelPlugin {
    @Override
    public String tabName() {
        return "Logs";
    }

    @Override
    public BottomPanelView view(EventBus eventBus) {
        return new LogsView();
    }
}
