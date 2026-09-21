package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.BorderLayout
import java.io.File
import java.io.IOException
import java.nio.file.Files

import org.xast.xide.core.event.EventBus
import org.xast.xide.core.event.FileSaveRequestedEvent
import org.xast.xide.core.plugin.file.FileModel
import org.xast.xide.core.plugin.file.TextFileModel
import org.xast.xide.core.plugin.ui.CodePanelView
import org.xast.xide.core.utils.Debug

class NeoEditorView(val eventBus: EventBus, val file: File) extends CodePanelView:
    private var neoEditor: NeoEditor = scala.compiletime.uninitialized
    private var editorStatus: NeoEditorStatus = scala.compiletime.uninitialized

    setLayout(new BorderLayout())

    private var content = ""

    if (file.exists() && file.canRead()) {
        try {
            content = Files.readString(file.toPath())
        } catch {
            case e: IOException =>
                Debug.error("Cannot read file `" + file.getName() + "`: " + e.getMessage())
        }
    }

    editorStatus = new NeoEditorStatus()
    add(editorStatus, BorderLayout.SOUTH)

    neoEditor = new NeoEditor(
        eventBus,
        content,
        editorStatus,
        () => eventBus.publish(new FileSaveRequestedEvent(file, false)),
    )
    add(neoEditor, BorderLayout.CENTER)

    override def model(): FileModel = new TextFileModel(neoEditor.getContent())
