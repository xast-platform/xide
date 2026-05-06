package org.xast.xide.kotlin_file_plugin

import java.io.File

import org.xast.xide.core.event.EventBus
import org.xast.xide.core.plugin.file.FilePlugin
import org.xast.xide.core.plugin.ui.CodePanelView
import org.xast.xide.ui.components.code_panel.EditorView
import org.xast.xide.ui.components.code_panel.neo_editor.NeoEditorView
import org.xast.xide.ui.utils.SyntaxStyle

class KotlinFilePlugin : FilePlugin {
    override fun fileExtensions(): Array<String> = arrayOf("kt")

    override fun view(eventBus: EventBus, file: File): CodePanelView =
        NeoEditorView()
}
