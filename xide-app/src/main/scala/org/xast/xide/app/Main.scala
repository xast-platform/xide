package org.xast.xide.app

import javax.swing.SwingUtilities

import org.xast.xide.core.PluginManager
import org.xast.xide.core.Workspace
import org.xast.xide.core.config.XideConfig
import org.xast.xide.core.event.EventBus
import org.xast.xide.core.utils.Debug
import org.xast.xide.ui.MainFrame
import org.xast.xide.ui.utils.XideStyle

object Main:
    def main(args: Array[String]): Unit = SwingUtilities.invokeLater(() =>
        XideStyle.setCurrent(XideStyle.defaultStyle)

        val pm = new PluginManager()
        pm.loadPlugins()

        val workspace = Workspace.init(args)
        if workspace.hasMultipleDirs then
            Debug.error("Workspace cannot contains multiple folders")
            System.exit(-1)

        val eventBus = new EventBus()
        val config = XideConfig.load(eventBus)
        val frame = new MainFrame(
            workspace,
            pm.getRegistry,
            eventBus,
            config
        )

        frame.loadPlugins()
        frame.setTitle("Xide")
        frame.show()
    )
