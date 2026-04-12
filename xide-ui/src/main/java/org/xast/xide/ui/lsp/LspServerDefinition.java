package org.xast.xide.ui.lsp;

import java.util.Set;

public record LspServerDefinition(
    String id,
    String command,
    String languageId,
    Set<String> fileExtensions
) {
}
