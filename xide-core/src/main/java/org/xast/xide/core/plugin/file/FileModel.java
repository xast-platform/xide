package org.xast.xide.core.plugin.file;

import java.io.File;
import java.io.IOException;

public interface FileModel {
    void saveToFile(File file) throws IOException;
}
