package org.xast.xide.core.plugin.file;

import java.io.File;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.plugin.ui.CodePanelView;
import org.xast.xide.core.plugin.ui.StatusLabel;

public interface FilePlugin {
    String fileExtension();

    CodePanelView view(EventBus eventBus, File file, StatusLabel statusLabel);
}
