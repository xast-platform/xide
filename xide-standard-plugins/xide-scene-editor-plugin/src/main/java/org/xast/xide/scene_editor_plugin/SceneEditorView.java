package org.xast.xide.scene_editor_plugin;

import java.io.File;
import java.io.IOException;
import java.awt.BorderLayout;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.plugin.file.FileModel;
import org.xast.xide.core.plugin.ui.CodePanelView;
import org.xast.xide.ui.components.CenteredLabel;

public class SceneEditorView extends CodePanelView {
    public static final String CLI_APP = "xastge-scene";

    private EventBus eventBus;
    private File file;

    public SceneEditorView(EventBus eventBus, File file) {
        this.eventBus = eventBus;
        this.file = file;

        setLayout(new BorderLayout());

        if (!isAppInstalled()) {
            add(new CenteredLabel("CLI app <b>"+CLI_APP+ "</b> is not installed.", 200, 18f));
            return;
        }
    }

    @Override
    public FileModel model() {
        return new SceneFileModel();
    }

    private boolean isAppInstalled() {
        try {
            Process process = new ProcessBuilder(CLI_APP, "--version")
                .redirectErrorStream(true)
                .start();

            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
