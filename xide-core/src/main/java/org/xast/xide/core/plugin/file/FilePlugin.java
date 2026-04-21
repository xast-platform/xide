package org.xast.xide.core.plugin.file;

import java.io.File;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.plugin.ui.CodePanelView;

public interface FilePlugin {
    String[] fileExtensions();

    CodePanelView view(EventBus eventBus, File file);
}
