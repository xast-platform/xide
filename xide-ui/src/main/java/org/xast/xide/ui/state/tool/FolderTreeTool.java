package org.xast.xide.ui.state.tool;

import org.xast.xide.core.Workspace;
import org.xast.xide.ui.component.code_panel.CodePanel;
import org.xast.xide.ui.component.side.SideBar;
import org.xast.xide.ui.component.side.view.SideBarView;
import org.xast.xide.ui.component.side.view.folder_tree.FolderTreeView;

public class FolderTreeTool implements Tool {
    private SideBar sideBar;
    private SideBarView view;
    private CodePanel codePanel;

    public FolderTreeTool(SideBar sideBar, CodePanel codePanel, Workspace workspace) {
        this.sideBar = sideBar;
        this.codePanel = codePanel;
        this.view = new FolderTreeView(codePanel, workspace);
    }

    public void setWorkspace(Workspace workspace) {
        this.view = new FolderTreeView(codePanel, workspace);
    }

    @Override
    public void show() {
        sideBar.setView(view);
    }
}
