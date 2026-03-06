package org.xast.xide.ui.components.code_panel;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CodePanelTabModel {
    private boolean saved;
    private File file;
}
