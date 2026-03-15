package org.xast.xide.core.event;

import java.io.File;

public record FileSaveRequestedEvent(File file, boolean saved) {
}
