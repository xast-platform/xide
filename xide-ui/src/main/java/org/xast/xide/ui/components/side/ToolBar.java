package org.xast.xide.ui.components.side;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.plugin.tool.ToolOrientation;

public class ToolBar extends JPanel {
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
                setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }}, 
            BorderLayout.WEST
        );

        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x424242)));
    }

    public void addToolButton(ToolButton button, ToolOrientation orientation, boolean runAtStartup) {
        Tool tool = button.getTool();
        tools.put(tool.getClass(), tool);
        
        switch (orientation) {
            case NORTH -> toolBoxNorth.add(button);
            case SOUTH -> toolBoxSouth.add(button);
        }

        if (runAtStartup) {
            tool.show();
        }
    }

    public Tool getTool(Class<? extends Tool> toolClass) {
        return tools.get(toolClass);
    }
}
