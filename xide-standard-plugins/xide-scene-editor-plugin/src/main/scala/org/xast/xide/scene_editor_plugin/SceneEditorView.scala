package org.xast.xide.scene_editor_plugin

import java.io.File
import java.io.IOException

import scala.util.Try
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension

import org.xast.xide.core.event.EventBus
import org.xast.xide.core.plugin.file.FileModel
import org.xast.xide.core.plugin.ui.CodePanelView
import org.xast.xide.ui.components.CenteredLabel

import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLJPanel

object SceneEditorView {
    val CLI_APP = "xastge-scene"
}

class SceneEditorView(
    val eventBus: EventBus, 
    val file: File
) extends CodePanelView() {
    setLayout(BorderLayout())

    if (!isAppInstalled()) {
        add(CenteredLabel("CLI app <b>$CLI_APP</b> is not installed.", 200, 18f))
    } else {
        add(createGLView(), BorderLayout.CENTER)
    }

    override def model(): FileModel = SceneFileModel()

    def createGLView(): Component = {
        val profile = GLProfile.get(GLProfile.GL4)
        val caps = GLCapabilities(profile)
        val panel = GLJPanel(caps)

        panel.addGLEventListener(new CubeRenderer())
        panel.setPreferredSize(new Dimension(800, 600))
        
        panel
    }

    def isAppInstalled(): Boolean = {
        Try {
            val process = ProcessBuilder(SceneEditorView.CLI_APP, "--version")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            exitCode == 0
        }.getOrElse(false)
    }
}
