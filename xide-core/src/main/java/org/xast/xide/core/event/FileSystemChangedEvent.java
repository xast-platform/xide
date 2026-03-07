package org.xast.xide.core.event;

import java.io.File;

public record FileSystemChangedEvent(File dir) {
}
