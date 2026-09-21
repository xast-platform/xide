package org.xast.xide.scene_editor_plugin

import java.io.File
import java.io.IOException

import org.xast.xide.core.plugin.file.FileModel

class SceneFileModel extends FileModel:

    @throws[IOException]
    override def saveToFile(file: File) = ()
