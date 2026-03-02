package org.xast.xide.ui.state.tool;

import org.xast.xide.ui.MainFrame;
import org.xast.xide.ui.dialog.SettingsDialog;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SettingsTool implements Tool {
    private final MainFrame frame;

    @Override
    public void show() {
        new SettingsDialog(frame).setVisible(true);
    }
}
