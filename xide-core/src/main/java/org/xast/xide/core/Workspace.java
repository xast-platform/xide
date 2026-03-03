package org.xast.xide.core;

import java.io.File;
import java.util.Arrays;

import org.xast.xide.core.Workspace.Blank;
import org.xast.xide.core.Workspace.Combined;
import org.xast.xide.core.Workspace.Directory;
import org.xast.xide.core.Workspace.ExistingFile;
import org.xast.xide.core.Workspace.NewFile;

public sealed interface Workspace 
    permits 
        Blank,
        Directory,
        ExistingFile,
        NewFile,
        Combined
{
    public boolean hasMultipleDirs();

    public static final record Blank() implements Workspace {
        public boolean hasMultipleDirs() {
            return false;
        }
    }

    public static final record Directory(File dir) implements Workspace {
        public boolean hasMultipleDirs() {
            return false;
        }
    }

    public static final record ExistingFile(File file) implements Workspace {
        public boolean hasMultipleDirs() {
            return false;
        }
    }

    public static final record NewFile(File file) implements Workspace {
        public boolean hasMultipleDirs() {
            return false;
        }
    }

    public static final record Combined(Workspace[] workspaces) implements Workspace {
        public boolean hasMultipleDirs() {
            return Arrays.stream(workspaces)
                .filter(ws -> ws instanceof Directory)
                .count() > 1;
        }
    }

    public static Workspace init(String[] args) {
        return switch (args.length) {
            case 0 -> new Blank();
            case 1 -> initSingle(args[0]);
            default -> new Combined(
                Arrays.stream(args)
                    .map(path -> initSingle(path))
                    .distinct()
                    .toArray(Workspace[]::new)
            );
        };
    }

    private static Workspace initSingle(String path) {
        File file = new File(path);

        if (file.exists()) {
            if (file.isDirectory()) {
                return new Directory(file);
            } else {
                return new ExistingFile(file);
            }
        } else {
            return new NewFile(file);
        }
    }
}
