package org.xast.xide.ui.state.tool;

import org.xast.xide.ui.component.side.SideBar;
import org.xast.xide.ui.component.side.view.FolderTreeView;
import org.xast.xide.ui.component.side.view.SideBarView;

public class FolderTreeTool implements Tool {
    private SideBar sideBar;
    private SideBarView view;

    public FolderTreeTool(SideBar sideBar) {
        this.sideBar = sideBar;
        this.view = new FolderTreeView();
    }

    @Override
    public void show() {
        sideBar.setView(view);
    }
}
