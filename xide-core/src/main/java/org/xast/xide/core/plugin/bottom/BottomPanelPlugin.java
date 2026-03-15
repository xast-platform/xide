package org.xast.xide.core.plugin.bottom;

import org.xast.xide.core.event.EventBus;

public interface BottomPanelPlugin {
    String tabName();

    BottomPanelView view(EventBus eventBus);
}
