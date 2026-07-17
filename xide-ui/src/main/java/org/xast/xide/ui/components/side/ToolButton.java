package org.xast.xide.ui.components.side;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.JButton;

import org.xast.xide.core.plugin.tool.Tool;
import org.xast.xide.core.utils.LucideIcon;
import org.xast.xide.ui.utils.XideStyle;

import lombok.Getter;

public class ToolButton extends JButton {
    @Getter
    private Tool tool;

    public ToolButton(LucideIcon icon, String tooltip, Tool tool) {
        super();
        this.tool = tool;

        XideStyle style = XideStyle.getCurrent();

        setToolTipText(tooltip);
        setMargin(new Insets(8,8,8,8));
        setIcon(icon.icon(24, style.isDarkTheme() ? Color.WHITE : Color.BLACK));
        addActionListener(e -> {
            tool.show();
        });
    }
}
