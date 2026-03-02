package org.xast.xide.ui.component.side;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JPanel;

import org.xast.xide.ui.state.tool.Tool;

public class ToolBar extends JPanel{
    private HashMap<Class<? extends Tool>, Tool> tools = new HashMap<>();
    private Box toolBoxNorth;
    private Box toolBoxSouth;

    public ToolBar() {
        setLayout(new BorderLayout());
        toolBoxNorth = Box.createVerticalBox();
        toolBoxSouth = Box.createVerticalBox();

        add(toolBoxNorth, BorderLayout.NORTH);
        add(toolBoxSouth, BorderLayout.SOUTH);
    }

    public void addToolButtonNorth(ToolButton button) {
        var tool = button.getTool();
        tools.put(tool.getClass(), tool);
        toolBoxNorth.add(button);
    }

    public void addToolButtonSouth(ToolButton button) {
        var tool = button.getTool();
        tools.put(tool.getClass(), tool);
        toolBoxSouth.add(button);
    }

    public void setDefaultTool(Class<? extends Tool> toolClass) {
        Tool tool = tools.get(toolClass);
        if (tool != null) {
            tool.show();
        }
    }

    public Tool getTool(Class<? extends Tool> toolClass) {
        return tools.get(toolClass);
    }
}
