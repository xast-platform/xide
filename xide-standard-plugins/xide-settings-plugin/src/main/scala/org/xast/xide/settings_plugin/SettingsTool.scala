package org.xast.xide.settings_plugin

import javax.swing.JFrame
import org.xast.xide.core.plugin.tool.Tool

class SettingsTool(val frame: JFrame) extends Tool {
    override def show(): Unit = {
        new SettingsDialog(this.frame).setVisible(true)
    }
}
