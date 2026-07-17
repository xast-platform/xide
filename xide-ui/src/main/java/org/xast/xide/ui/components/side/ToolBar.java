package org.xast.xide.ui.components.side;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.ThemeChangedEvent;
import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.plugin.tool.ToolOrientation;
import org.xast.xide.ui.utils.XideStyle;

public class ToolBar extends JPanel {
    private HashMap<Class<? extends Tool>, Tool> tools = new HashMap<>();
    private Box toolBoxNorth;
    private Box toolBoxSouth;
    private XideStyle style;

    public ToolBar(EventBus eventBus) {
        setLayout(new BorderLayout());
        toolBoxNorth = Box.createVerticalBox();
        toolBoxSouth = Box.createVerticalBox();
        style = XideStyle.getCurrent();

        add(
            new JPanel(new BorderLayout()) {{
                add(toolBoxNorth, BorderLayout.NORTH);
                add(toolBoxSouth, BorderLayout.SOUTH);
                setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }},
            BorderLayout.WEST
        );

        applyTheme();

        eventBus.subscribe(ThemeChangedEvent.class, e -> applyTheme());
    }

    private void applyTheme() {
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, style.shiftAccent(-0.25f)));
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
