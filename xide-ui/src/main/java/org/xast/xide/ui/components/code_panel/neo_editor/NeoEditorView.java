package org.xast.xide.ui.components.code_panel.neo_editor;

import org.xast.xide.core.plugin.file.FileModel;
import org.xast.xide.core.plugin.file.TextFileModel;
import org.xast.xide.core.plugin.ui.CodePanelView;

public class NeoEditorView extends CodePanelView {
    

    @Override
    public FileModel model() {
        return new TextFileModel("");
    }
}
