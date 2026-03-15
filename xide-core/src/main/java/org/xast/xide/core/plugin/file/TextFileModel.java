package org.xast.xide.core.plugin.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TextFileModel implements FileModel {
    private String content;

    public TextFileModel(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public void saveToFile(File file) throws IOException {
        Files.writeString(file.toPath(), content);
    }
}
