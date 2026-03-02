package org.xast.xide.ui.component.side;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;

import org.xast.xide.ui.component.side.view.SideBarView;


public class SideBar extends JPanel {
    private SideBarView currentView;
    private List<ToolButton> toolButtons = new ArrayList<>();
    private Box toolBoxNorth;
    private Box toolBoxSouth;

    public SideBar() {
        super(new BorderLayout());

        var leftPanel = new JPanel(new BorderLayout());
        toolBoxNorth = Box.createVerticalBox();
        toolBoxSouth = Box.createVerticalBox();

        leftPanel.add(toolBoxNorth, BorderLayout.NORTH);
        leftPanel.add(toolBoxSouth, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
    }

    public void addToolButtonNorth(ToolButton button) {
        toolButtons.add(button);
        toolBoxNorth.add(button);
    }

    public void addToolButtonSouth(ToolButton button) {
        toolButtons.add(button);
        toolBoxSouth.add(button);
    }

    public void setView(SideBarView view) {
        var oldView = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (oldView != null) {
            remove(oldView);
        }

        if (view != currentView) {
            currentView = view;
            add(view);
        } 
    }
}
