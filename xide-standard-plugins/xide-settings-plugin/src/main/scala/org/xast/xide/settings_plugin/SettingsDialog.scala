package org.xast.xide.settings_plugin

import java.awt.BorderLayout
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel

class SettingsDialog(owner: JFrame) extends JDialog(owner, "Settings", true) {
    setSize(700, 500)
    setLocationRelativeTo(owner)
    setLayout(new BorderLayout())

    add(new JLabel("Settings go here"), BorderLayout.CENTER)
}