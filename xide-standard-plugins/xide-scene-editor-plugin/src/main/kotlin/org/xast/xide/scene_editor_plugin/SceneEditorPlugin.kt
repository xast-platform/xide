package org.xast.xide.scene_editor_plugin

import java.io.File

import org.xast.xide.core.event.EventBus
import org.xast.xide.core.plugin.file.FilePlugin
import org.xast.xide.core.plugin.ui.CodePanelView

class SceneEditorPlugin : FilePlugin {
    override fun fileExtensions(): Array<String> = arrayOf("xsc")

    override fun view(eventBus: EventBus, file: File): CodePanelView =
        SceneEditorView(eventBus, file)
}
