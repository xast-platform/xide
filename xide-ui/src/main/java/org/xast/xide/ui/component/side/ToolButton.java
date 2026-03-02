package org.xast.xide.ui.component.side;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.JButton;

import org.xast.xide.ui.state.tool.Tool;
import org.xast.xide.ui.utils.LucideIcon;

import lombok.Getter;

public class ToolButton extends JButton {
    @Getter
    private Tool tool;

    public ToolButton(LucideIcon icon, String tooltip, Tool tool) {
        super();
        this.tool = tool;
        
        setToolTipText(tooltip);
        setMargin(new Insets(8,8,8,8));
        setIcon(icon.icon(28, Color.WHITE));
        addActionListener(e -> {
            tool.show();
        });
    }
}
