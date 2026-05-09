package org.xast.xide.ui.components.code_panel.neo_editor;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.FileSaveRequestedEvent;
import org.xast.xide.core.plugin.file.FileModel;
import org.xast.xide.core.plugin.file.TextFileModel;
import org.xast.xide.core.plugin.ui.CodePanelView;
import org.xast.xide.core.utils.Debug;

public class NeoEditorView extends CodePanelView {
    private NeoEditor neoEditor;
    private NeoEditorStatus editorStatus;

    public NeoEditorView(EventBus eventBus, File file) {
        setLayout(new BorderLayout());

        String content = new String();

        if (file.exists() && file.canRead()) {
            try {
                content = Files.readString(file.toPath());
            } catch (IOException e) { 
                Debug.error("Cannot read file `"+file.getName()+"`: "+e.getMessage());
            }
        }

        editorStatus = new NeoEditorStatus();
        add(editorStatus, BorderLayout.SOUTH);

        neoEditor = new NeoEditor(
            content, 
            editorStatus, 
            () -> {
                eventBus.publish(new FileSaveRequestedEvent(file, false));
            }
        );
        add(neoEditor, BorderLayout.CENTER);
    }

    @Override
    public FileModel model() {
        return new TextFileModel(neoEditor.getContent());
    }
}
