package org.xast.xide.ui.dialog;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;

import org.xast.xide.ui.MainFrame;

public class SettingsDialog extends JDialog {
    public SettingsDialog(MainFrame owner) {
        super(owner.getFrame(), "Settings", true); // modal

        setSize(700, 500);
        setLocationRelativeTo(owner.getFrame());
        setLayout(new BorderLayout());

        add(new JLabel("Settings go here"), BorderLayout.CENTER);
    }
}
