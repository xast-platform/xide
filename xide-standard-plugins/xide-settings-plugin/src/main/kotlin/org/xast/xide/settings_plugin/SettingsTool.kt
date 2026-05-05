package org.xast.xide.settings_plugin

import javax.swing.JFrame
import org.xast.xide.core.plugin.tool.Tool

class SettingsTool(val frame: JFrame) : Tool {
    override fun show() {
        SettingsDialog(this.frame).isVisible = true
    }
}
