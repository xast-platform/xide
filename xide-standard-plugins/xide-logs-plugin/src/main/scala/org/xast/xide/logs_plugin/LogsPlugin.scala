package org.xast.xide.logs_plugin

import org.xast.xide.core.event.EventBus
import org.xast.xide.core.plugin.bottom.BottomPanelPlugin
import org.xast.xide.core.plugin.bottom.BottomPanelView

class LogsPlugin extends BottomPanelPlugin:
    
    override def tabName: String = "Logs"

    override def view(eventBus: EventBus): BottomPanelView = new LogsView()