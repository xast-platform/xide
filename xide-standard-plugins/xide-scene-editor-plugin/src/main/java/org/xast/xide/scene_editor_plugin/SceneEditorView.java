package org.xast.xide.scene_editor_plugin;

import java.io.File;
import java.io.IOException;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.plugin.file.FileModel;
import org.xast.xide.core.plugin.ui.CodePanelView;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;

public class SceneEditorView extends CodePanelView {
    public static final String CLI_APP = "xastge-scene";

    @SuppressWarnings("unused")
    private EventBus eventBus;
    @SuppressWarnings("unused")
    private File file;

    public SceneEditorView(EventBus eventBus, File file) {
        this.eventBus = eventBus;
        this.file = file;

        setLayout(new BorderLayout());

        // if (!isAppInstalled()) {
        //     add(new CenteredLabel("CLI app <b>"+CLI_APP+ "</b> is not installed.", 200, 18f));
        //     return;
        // }
        
        add(createGLView(), BorderLayout.CENTER);
    }

    private Component createGLView() {
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);

        GLJPanel panel = new GLJPanel(caps);

        panel.addGLEventListener(new CubeRenderer());
        panel.setPreferredSize(new Dimension(800, 600));
        return panel;
    }

    @Override
    public FileModel model() {
        return new SceneFileModel();
    }

    @SuppressWarnings("unused")
    private boolean isAppInstalled() {
        try {
            Process process = new ProcessBuilder(CLI_APP, "--version")
                .redirectErrorStream(true)
                .start();

            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
