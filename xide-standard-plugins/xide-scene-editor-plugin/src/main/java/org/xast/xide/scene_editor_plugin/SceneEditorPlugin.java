package org.xast.xide.scene_editor_plugin;

import java.io.File;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.plugin.file.FilePlugin;
import org.xast.xide.core.plugin.ui.CodePanelView;
import org.xast.xide.core.plugin.ui.StatusLabel;

public class SceneEditorPlugin implements FilePlugin {
    @Override
    public String fileExtension() {
        return "xsc";
    }

    @Override
    public CodePanelView view(EventBus eventBus, File file, StatusLabel statusLabel) {
        return new SceneEditorView(eventBus, file);
    }
}
