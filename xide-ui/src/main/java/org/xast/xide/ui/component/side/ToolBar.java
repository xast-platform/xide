package org.xast.xide.ui.component.side;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.xast.xide.ui.state.tool.Tool;
import org.xast.xide.utils.Debug;

public class ToolBar extends JPanel{
    private HashMap<Class<? extends Tool>, Tool> tools = new HashMap<>();
    private Box toolBoxNorth;
    private Box toolBoxSouth;

    public ToolBar() {
        setLayout(new BorderLayout());
        toolBoxNorth = Box.createVerticalBox();
        toolBoxSouth = Box.createVerticalBox();

        add(
            new JPanel(new BorderLayout()) {{
                add(toolBoxNorth, BorderLayout.NORTH);
                add(toolBoxSouth, BorderLayout.SOUTH);
            }}, 
            BorderLayout.WEST
        );

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(sep.getForeground().darker());
        add(sep, BorderLayout.EAST);
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
