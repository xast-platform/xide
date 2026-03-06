package org.xast.xide.settings_plugin;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class SettingsDialog extends JDialog {
    public SettingsDialog(JFrame owner) {
        super(owner, "Settings", true); // modal

        setSize(700, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(new JLabel("Settings go here"), BorderLayout.CENTER);
    }
}
