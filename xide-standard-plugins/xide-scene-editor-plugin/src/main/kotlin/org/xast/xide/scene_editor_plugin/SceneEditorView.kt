package org.xast.xide.scene_editor_plugin

import java.io.File
import java.io.IOException

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

class SceneEditorView(val eventBus: EventBus, val file: File) : CodePanelView() {
    companion object {
        const val CLI_APP = "xastge-scene"
    }

    init {
        layout = BorderLayout()

        if (!isAppInstalled()) {
            add(CenteredLabel("CLI app <b>$CLI_APP</b> is not installed.", 200, 18f))
        } else {
            add(createGLView(), BorderLayout.CENTER)
        }
    }

    fun createGLView(): Component {
        val profile = GLProfile.get(GLProfile.GL4)
        val caps = GLCapabilities(profile)

        val panel = GLJPanel(caps)

        panel.addGLEventListener(CubeRenderer())
        panel.preferredSize = Dimension(800, 600)
        
        return panel
    }

    override fun model(): FileModel = SceneFileModel()

    fun isAppInstalled(): Boolean {
        try {
            val process = ProcessBuilder(CLI_APP, "--version")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            return exitCode == 0
        } catch (e: Exception) {
            return false
        }
    }
}
