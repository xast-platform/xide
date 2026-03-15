package org.xast.xide.core.plugin.file;

import java.io.File;
import java.io.IOException;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.plugin.ui.CodePanelView;

public interface FilePlugin {
    String fileExtension();

    CodePanelView view(EventBus eventBus, File file);

    void save(FileModel model, File file) throws IOException;
}
