package org.xast.xide.ui.components.side;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.JButton;

import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.utils.LucideIcon;

import lombok.Getter;

public class ToolButton extends JButton {
    @Getter
    private Tool tool;

    public ToolButton(LucideIcon icon, String tooltip, Tool tool) {
        super();
        this.tool = tool;
        
        setToolTipText(tooltip);
        setMargin(new Insets(8,8,8,8));
        setIcon(icon.icon(24, Color.WHITE));
        addActionListener(e -> {
            tool.show();
        });
    }
}
