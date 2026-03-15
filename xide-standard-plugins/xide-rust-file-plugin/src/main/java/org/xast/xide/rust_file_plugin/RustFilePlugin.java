package org.xast.xide.rust_file_plugin;

import java.io.File;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.plugin.file.FilePlugin;
import org.xast.xide.core.plugin.ui.CodePanelView;
import org.xast.xide.ui.components.code_panel.EditorView;
import org.xast.xide.ui.utils.SyntaxStyle;

public class RustFilePlugin implements FilePlugin {
    @Override
    public String fileExtension() {
        return "rs";
    }

    @Override
    public CodePanelView view(EventBus eventBus, File file) {
        return new EditorView(eventBus, file, SyntaxStyle.Rust, 4);
    }
}
