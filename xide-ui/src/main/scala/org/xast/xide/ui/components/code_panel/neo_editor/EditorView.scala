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

class EditorView(val eventBus: EventBus, val file: File) extends CodePanelView:
   
   private var editor: EditorComponent = scala.compiletime.uninitialized
   private var editorStatus: EditorStatus = scala.compiletime.uninitialized

   setLayout(new BorderLayout())

   private var content = ""

   if file.exists() && file.canRead() then
      try
         content = Files.readString(file.toPath())
      catch
         case e: IOException =>
            Debug.error("Cannot read file `" + file.getName() + "`: " + e.getMessage())

   editorStatus = new EditorStatus()
   add(editorStatus.peer, BorderLayout.SOUTH)

   editor = new EditorComponent(
      content,
      editorStatus,
      () => eventBus.publish(new FileSaveRequestedEvent(file, false)),
   )
   add(editor.peer, BorderLayout.CENTER)

   override def model(): FileModel = new TextFileModel(editor.getContent)
