package org.xast.xide.logs_plugin

import org.xast.xide.core.event.EventBus
import org.xast.xide.core.plugin.bottom.BottomPanelPlugin
import org.xast.xide.core.plugin.bottom.BottomPanelView

class LogsPlugin : BottomPanelPlugin {
    override fun tabName(): String = "Logs"

    override fun view(eventBus: EventBus): BottomPanelView = LogsView()
}