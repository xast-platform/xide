package org.xast.xide.ui.components.code_panel;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CodePanelTabModel {
    private boolean saved;
    private File file;
}
