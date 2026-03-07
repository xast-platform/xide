package org.xast.xide.core;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

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
    public Workspace withFile(File file);
    public Optional<File> getDirectory();

    public static final record Blank() implements Workspace {
        public boolean hasMultipleDirs() {
            return false;
        }

        public Workspace withFile(File file) {
            if (file.exists()) {
                return new ExistingFile(file);
            } else {
                return new NewFile(file);
            }
        }

        public Optional<File> getDirectory() {
            return Optional.empty();
        }
    }

    public static final record Directory(File dir) implements Workspace {
        public boolean hasMultipleDirs() {
            return false;
        }

        public Workspace withFile(File file) {
            if (file.exists()) {
                return new Combined(new Workspace[] {
                    this,
                    new ExistingFile(file)
                });
            } else {
                return new Combined(new Workspace[] {
                    this,
                    new NewFile(file)
                });
            }
        }

        public Optional<File> getDirectory() {
            return Optional.of(dir);
        }
    }

    public static final record ExistingFile(File file) implements Workspace {
        public boolean hasMultipleDirs() {
            return false;
        }

        public Workspace withFile(File file) {
            if (file.exists()) {
                return new Combined(new Workspace[] {
                    this,
                    new ExistingFile(file)
                });
            } else {
                return new Combined(new Workspace[] {
                    this,
                    new NewFile(file)
                });
            }
        }

        public Optional<File> getDirectory() {
            return Optional.empty();
        }
    }

    public static final record NewFile(File file) implements Workspace {
        public boolean hasMultipleDirs() {
            return false;
        }

        public Workspace withFile(File file) {
            if (file.exists()) {
                return new Combined(new Workspace[] {
                    this,
                    new ExistingFile(file)
                });
            } else {
                return new Combined(new Workspace[] {
                    this,
                    new NewFile(file)
                });
            }
        }

        public Optional<File> getDirectory() {
            return Optional.empty();
        }
    }

    public static final record Combined(Workspace[] workspaces) implements Workspace {
        public boolean hasMultipleDirs() {
            return Arrays.stream(workspaces)
                .filter(ws -> ws instanceof Directory)
                .count() > 1;
        }

        public Workspace withFile(File file) {
            Workspace[] newWorkspaces = Arrays.copyOf(workspaces, workspaces.length + 1);
            if (file.exists()) {
                newWorkspaces[workspaces.length] = new ExistingFile(file);
            } else {
                newWorkspaces[workspaces.length] = new NewFile(file);
            }
            return new Combined(newWorkspaces);
        }

        public Optional<File> getDirectory() {
            return Arrays.stream(workspaces)
                .filter(ws -> ws instanceof Directory)
                .map(ws -> ((Directory) ws).dir())
                .findAny();
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
