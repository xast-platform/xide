package org.xast.xide.settings_plugin

import java.awt.BorderLayout
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel

class SettingsDialog(owner: JFrame) : JDialog(owner, "Settings", true) {
    init {
        setSize(700, 500)
        setLocationRelativeTo(owner)
        layout = BorderLayout()

        add(JLabel("Settings go here"), BorderLayout.CENTER)
    }
}