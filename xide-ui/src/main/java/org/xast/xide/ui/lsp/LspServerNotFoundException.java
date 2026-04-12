package org.xast.xide.ui.lsp;

public class LspServerNotFoundException extends Exception {
    public LspServerNotFoundException(String name) {
        super("LSP server app name called `"+name+"` not found");
    }
}
