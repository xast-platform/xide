package org.xast.xide.settings_plugin;

import javax.swing.JFrame;

import org.xast.xide.core.plugin.tool.Tool;

public class SettingsTool implements Tool {
    private final JFrame frame;

    public SettingsTool(JFrame frame) {
        this.frame = frame;
    }

    @Override
    public void show() {
        new SettingsDialog(frame)
            .setVisible(true);
    }
}
